from app.llm import LanguageModel
from app.models import (
    RagAnswerRequest,
    RagAnswerResponse,
    RagAnswerSource,
    SemanticSearchRequest,
    SemanticSearchResult,
)
from app.search_service import SemanticSearchService


INSUFFICIENT_EVIDENCE_ANSWER = "검색된 위키 문서만으로는 질문에 답하기 위한 근거가 충분하지 않습니다."

RAG_INSTRUCTIONS = """당신은 대학 위키 문서만을 근거로 답하는 도우미입니다.
제공된 컨텍스트에 명시된 정보만 사용하세요.
컨텍스트에 없는 사실을 추측하거나 일반 지식으로 보완하지 마세요.
근거가 부족하거나 서로 충돌하면 근거가 충분하지 않다고 명확히 답하세요.
답변은 한국어로 간결하고 직접적으로 작성하세요."""


class RagAnswerService:
    def __init__(
        self,
        search_service: SemanticSearchService,
        language_model: LanguageModel,
        top_k: int,
        min_score: float,
        max_context_chars: int,
    ) -> None:
        self.search_service = search_service
        self.language_model = language_model
        self.top_k = top_k
        self.min_score = min_score
        self.max_context_chars = max_context_chars

    def answer(self, request: RagAnswerRequest) -> RagAnswerResponse:
        search_response = self.search_service.search(
            SemanticSearchRequest(
                query=request.question,
                topK=self.top_k,
                categoryId=request.category_id,
            )
        )
        relevant_results = [
            result
            for result in search_response.results
            if result.score >= self.min_score
        ]
        if not relevant_results:
            return RagAnswerResponse(
                question=request.question,
                answer=INSUFFICIENT_EVIDENCE_ANSWER,
                grounded=False,
                retrievedChunkCount=0,
                sources=[],
            )

        context, used_results = self._build_context(relevant_results)
        answer = self.language_model.generate(
            RAG_INSTRUCTIONS,
            f"질문:\n{request.question}\n\n위키 컨텍스트:\n{context}",
        )
        return RagAnswerResponse(
            question=request.question,
            answer=answer,
            grounded=True,
            retrievedChunkCount=len(used_results),
            sources=self._build_sources(used_results),
        )

    def _build_context(
        self,
        results: list[SemanticSearchResult],
    ) -> tuple[str, list[SemanticSearchResult]]:
        sections = []
        used_results = []
        current_length = 0
        for index, result in enumerate(results, start=1):
            section = f"[문서 {index}]\n제목: {result.title}\n내용: {result.content}\n"
            remaining = self.max_context_chars - current_length
            if remaining <= 0:
                break
            sections.append(section[:remaining])
            used_results.append(result)
            current_length += len(sections[-1])
        return "\n".join(sections), used_results

    @staticmethod
    def _build_sources(
        results: list[SemanticSearchResult],
    ) -> list[RagAnswerSource]:
        sources = []
        seen_wiki_post_ids = set()
        for result in results:
            if result.wiki_post_id in seen_wiki_post_ids:
                continue
            seen_wiki_post_ids.add(result.wiki_post_id)
            sources.append(
                RagAnswerSource(
                    wikiPostId=result.wiki_post_id,
                    title=result.title,
                    url=f"/api/wiki-posts/{result.wiki_post_id}",
                )
            )
        return sources
