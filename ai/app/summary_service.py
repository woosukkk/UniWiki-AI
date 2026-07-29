from app.llm import LanguageModel
from app.models import VectorStoreRecord, WikiSummaryResponse
from app.vector_store import VectorStore


SUMMARY_INSTRUCTIONS = """당신은 대학 위키 문서를 정확하고 간결하게 요약하는 도우미입니다.
제공된 문서에 명시된 정보만 사용하세요.
핵심 규칙, 일정, 절차, 장소와 주의사항을 우선해서 정리하세요.
원문에 없는 내용을 추측하지 말고 한국어로 작성하세요."""


class WikiDocumentNotFoundError(LookupError):
    pass


class WikiSummaryService:
    def __init__(
        self,
        vector_store: VectorStore,
        language_model: LanguageModel,
        default_max_chars: int,
        context_chars: int,
    ) -> None:
        self.vector_store = vector_store
        self.language_model = language_model
        self.default_max_chars = default_max_chars
        self.context_chars = context_chars

    def summarize(
        self,
        wiki_post_id: int,
        max_chars: int | None = None,
    ) -> WikiSummaryResponse:
        document = self.vector_store.get_document(wiki_post_id)
        if not document.chunks:
            raise WikiDocumentNotFoundError(
                f"Wiki document {wiki_post_id} is not in the vector store."
            )

        limit = max_chars or self.default_max_chars
        batches = self._build_batches(document.chunks)
        summaries = [self._summarize_text(batch, limit) for batch in batches]
        while len(summaries) > 1:
            summaries = [
                self._summarize_text(batch, limit)
                for batch in self._group_texts(summaries)
            ]

        return WikiSummaryResponse(
            wikiPostId=wiki_post_id,
            title=document.chunks[0].metadata.title,
            summary=summaries[0][:limit].rstrip(),
            sourceChunkCount=len(document.chunks),
        )

    def _summarize_text(self, text: str, max_chars: int) -> str:
        prompt = (
            f"다음 내용을 {max_chars}자 이내로 요약하세요. "
            "중요한 내용을 빠뜨리지 않되 반복은 제거하세요.\n\n"
            f"{text}"
        )
        return self.language_model.generate(SUMMARY_INSTRUCTIONS, prompt)[:max_chars].rstrip()

    def _build_batches(self, chunks: list[VectorStoreRecord]) -> list[str]:
        return self._group_texts([chunk.text for chunk in chunks])

    def _group_texts(self, texts: list[str]) -> list[str]:
        batches = []
        current = ""
        for text in texts:
            text = text.strip()
            if not text:
                continue
            if current and len(current) + len(text) + 2 > self.context_chars:
                batches.append(current)
                current = ""
            while len(text) > self.context_chars:
                if current:
                    batches.append(current)
                    current = ""
                batches.append(text[: self.context_chars])
                text = text[self.context_chars :]
            current = f"{current}\n\n{text}".strip() if current else text
        if current:
            batches.append(current)
        return batches
