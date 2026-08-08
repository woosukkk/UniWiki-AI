from functools import lru_cache
import re

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
질문의 핵심 대상에 대한 설명이 문서에 직접 있어야 근거가 충분한 것으로 판단하세요.
기간이나 일정은 문서 제목의 연도와 학기를 답변에 명시하세요.
첫 줄에는 반드시 `판정: SUPPORTED` 또는 `판정: INSUFFICIENT` 중 하나만 작성하세요.
그 다음 줄부터 사용자에게 보여줄 답변을 한국어로 간결하고 직접적으로 작성하세요."""

QUERY_REWRITE_INSTRUCTIONS = """당신은 대학 공식 문서 검색 질의 재작성기입니다.
사용자 질문의 의미, 고유명사, 학과명, 연도와 학기를 반드시 보존하세요.
공식 공지에서 사용할 법한 동의어, 상위어, 관련 행정 용어를 자연스럽게 보충하세요.
질문에 답하거나 새로운 사실을 추가하지 마세요.
검색에 사용할 한 줄의 문구만 출력하고 설명, 목록, 따옴표는 출력하지 마세요."""


class RagAnswerService:
    def __init__(
        self,
        search_service: SemanticSearchService,
        language_model: LanguageModel,
        top_k: int,
        min_score: float,
        partial_min_score: float,
        max_context_chars: int,
    ) -> None:
        self.search_service = search_service
        self.language_model = language_model
        self.top_k = top_k
        self.min_score = min_score
        self.partial_min_score = partial_min_score
        self.max_context_chars = max_context_chars

    def answer(self, request: RagAnswerRequest) -> RagAnswerResponse:
        rewritten_query = self._rewrite_query(request.question)
        search_query = self._combined_search_query(request.question, rewritten_query)
        search_response = self.search_service.search(
            SemanticSearchRequest(
                query=search_query,
                topK=self.top_k,
                categoryId=request.category_id,
            )
        )
        strong_results = [
            result
            for result in search_response.results
            if result.score >= self.min_score
        ]
        partial_results = [
            result
            for result in search_response.results
            if result.score >= self.partial_min_score
        ]
        relevant_results = strong_results or partial_results
        if not relevant_results:
            return RagAnswerResponse(
                question=request.question,
                answer=INSUFFICIENT_EVIDENCE_ANSWER,
                grounded=False,
                retrievedChunkCount=0,
                sources=[],
            )

        context_results = (
            self.search_service.expand_results(relevant_results)
            if hasattr(self.search_service, "expand_results")
            else relevant_results
        )
        context, used_results = self._build_context(context_results)
        evidence_notice = (
            "\n\n주의: 검색 점수가 부분 근거 구간입니다. "
            "문서에 직접 적힌 사실만 소개하고 전체 정보라고 단정하지 마세요."
            if not strong_results else ""
        )
        generated = self.language_model.generate(
            RAG_INSTRUCTIONS,
            f"질문:\n{request.question}\n\n위키 컨텍스트:\n{context}{evidence_notice}",
        )
        answer, grounded = self._parse_grounding(generated)
        return RagAnswerResponse(
            question=request.question,
            answer=answer,
            grounded=grounded and bool(strong_results),
            retrievedChunkCount=len(used_results),
            sources=self._build_sources(used_results),
        )

    @lru_cache(maxsize=256)
    def _rewrite_query(self, question: str) -> str:
        generated = self.language_model.generate(
            QUERY_REWRITE_INSTRUCTIONS,
            f"사용자 질문:\n{question}",
        )
        normalized = " ".join(generated.replace("`", "").replace('"', "").split())
        return normalized[:500] or question

    @staticmethod
    def _combined_search_query(question: str, rewritten: str) -> str:
        if rewritten.strip().lower() == question.strip().lower():
            return question
        if not re.search(r"20\d{2}", question):
            rewritten = re.sub(
                r"20\d{2}(?:\s*[-\uB144.]?\s*[12](?:\s*\uD559\uAE30)?)?",
                "",
                rewritten,
            )
        return f"{question} {rewritten}"

    @staticmethod
    def _parse_grounding(generated: str) -> tuple[str, bool]:
        verdict = re.match(
            r"^\s*판정\s*:\s*(SUPPORTED|INSUFFICIENT)\s*\n?",
            generated,
            flags=re.IGNORECASE,
        )
        answer = generated[verdict.end():].strip() if verdict else generated.strip()
        if verdict:
            return answer, verdict.group(1).upper() == "SUPPORTED"

        insufficient_patterns = (
            "확인할 수 없습니다",
            "알 수 없습니다",
            "근거가 충분하지",
            "설명은 없습니다",
            "정보가 없습니다",
        )
        return answer, not any(pattern in answer for pattern in insufficient_patterns)

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
        seen_titles = []
        for result in results:
            normalized_title = SemanticSearchService._canonical_title(result.title)
            if (result.wiki_post_id in seen_wiki_post_ids
                    or any(SemanticSearchService._titles_are_duplicates(
                        normalized_title, seen,
                    ) for seen in seen_titles)):
                continue
            seen_wiki_post_ids.add(result.wiki_post_id)
            seen_titles.append(normalized_title)
            sources.append(
                RagAnswerSource(
                    wikiPostId=result.wiki_post_id,
                    title=result.title,
                    url=f"/api/wiki-posts/{result.wiki_post_id}",
                )
            )
        return sources
