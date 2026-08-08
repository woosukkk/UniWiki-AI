from fastapi.testclient import TestClient

from app.llm import LlmConfigurationError, LlmTimeoutError
from app.main import app, get_rag_service
from app.models import SemanticSearchResponse, SemanticSearchResult
from app.rag_service import INSUFFICIENT_EVIDENCE_ANSWER, RagAnswerService


class FakeSearchService:
    def __init__(self, results):
        self.results = results
        self.request = None

    def search(self, request):
        self.request = request
        return SemanticSearchResponse(query=request.query, topK=request.top_k or 5, resultCount=len(self.results), results=self.results)


class FakeLanguageModel:
    def __init__(self, answer="포털의 수강신청 메뉴에서 신청할 수 있습니다.", error=None,
                 rewrite="수강신청 신청 절차 포털 안내"):
        self.answer = answer
        self.error = error
        self.rewrite = rewrite
        self.calls = []

    def generate(self, instructions, input_text):
        self.calls.append((instructions, input_text))
        if self.error:
            raise self.error
        if "검색 질의 재작성기" in instructions:
            return self.rewrite
        return self.answer


def search_result(score=0.91, chunk_index=0):
    return SemanticSearchResult(chunkId=f"wiki-7-chunk-{chunk_index}", wikiPostId=7, title="수강신청 안내", content="수강신청은 학교 포털의 수강신청 메뉴에서 진행합니다.", categoryId=2, chunkIndex=chunk_index, score=score)


def override_service(results, language_model=None):
    search_service = FakeSearchService(results)
    language_model = language_model or FakeLanguageModel()
    service = RagAnswerService(search_service, language_model, top_k=4, min_score=0.4, max_context_chars=1000)
    app.dependency_overrides[get_rag_service] = lambda: service
    return search_service, language_model


def test_generates_answer_from_retrieved_wiki_context() -> None:
    search_service, language_model = override_service([search_result()])
    response = TestClient(app).post("/api/rag/answers", json={"question": "수강신청은 어디서 하나요?", "categoryId": 2})
    app.dependency_overrides.clear()
    assert response.status_code == 200
    assert response.json() == {
        "question": "수강신청은 어디서 하나요?",
        "answer": "포털의 수강신청 메뉴에서 신청할 수 있습니다.",
        "grounded": True,
        "retrievedChunkCount": 1,
        "sources": [
            {
                "wikiPostId": 7,
                "title": "수강신청 안내",
                "url": "/api/wiki-posts/7",
            }
        ],
    }
    assert search_service.request.top_k == 4
    assert search_service.request.category_id == 2
    assert "수강신청 안내" in language_model.calls[-1][1]


def test_does_not_generate_answer_when_evidence_is_insufficient() -> None:
    _, language_model = override_service([search_result(score=0.2)])
    response = TestClient(app).post("/api/rag/answers", json={"question": "등록금은 얼마인가요?"})
    app.dependency_overrides.clear()
    assert response.status_code == 200
    assert response.json()["answer"] == INSUFFICIENT_EVIDENCE_ANSWER
    assert response.json()["grounded"] is False
    assert len(language_model.calls) == 1
    assert "검색 질의 재작성기" in language_model.calls[0][0]
    assert response.json()["sources"] == []


def test_deduplicates_sources_from_chunks_of_the_same_wiki_post() -> None:
    override_service([search_result(chunk_index=0), search_result(chunk_index=1)])
    response = TestClient(app).post(
        "/api/rag/answers",
        json={"question": "수강신청은 어디서 하나요?"},
    )
    app.dependency_overrides.clear()

    assert response.status_code == 200
    assert response.json()["retrievedChunkCount"] == 2
    assert response.json()["sources"] == [
        {
            "wikiPostId": 7,
            "title": "수강신청 안내",
            "url": "/api/wiki-posts/7",
        }
    ]


def test_uses_expanded_chunks_from_selected_wiki_document() -> None:
    class ExpandingSearchService(FakeSearchService):
        def expand_results(self, results):
            return [search_result(chunk_index=0), search_result(chunk_index=1)]

    search_service = ExpandingSearchService([search_result(chunk_index=0)])
    language_model = FakeLanguageModel()
    service = RagAnswerService(
        search_service,
        language_model,
        top_k=4,
        min_score=0.4,
        max_context_chars=1000,
    )
    app.dependency_overrides[get_rag_service] = lambda: service

    response = TestClient(app).post(
        "/api/rag/answers",
        json={"question": "장학금 종류를 모두 알려줘"},
    )
    app.dependency_overrides.clear()

    assert response.status_code == 200
    assert response.json()["retrievedChunkCount"] == 2
    assert "[문서 2]" in language_model.calls[-1][1]


def test_returns_service_unavailable_when_api_key_is_missing() -> None:
    override_service([search_result()], FakeLanguageModel(error=LlmConfigurationError("missing")))
    response = TestClient(app).post("/api/rag/answers", json={"question": "수강신청은 어디서 하나요?"})
    app.dependency_overrides.clear()
    assert response.status_code == 503


def test_returns_gateway_timeout_for_llm_timeout() -> None:
    override_service([search_result()], FakeLanguageModel(error=LlmTimeoutError("timeout")))
    response = TestClient(app).post("/api/rag/answers", json={"question": "수강신청은 어디서 하나요?"})
    app.dependency_overrides.clear()
    assert response.status_code == 504


def test_rejects_blank_question() -> None:
    assert TestClient(app).post("/api/rag/answers", json={"question": "   "}).status_code == 422


def test_model_insufficient_verdict_marks_answer_as_not_grounded() -> None:
    model = FakeLanguageModel(
        answer="판정: INSUFFICIENT\n제공된 문서에는 서비스의 목적과 운영 방식이 없습니다."
    )
    override_service([search_result()], model)

    response = TestClient(app).post(
        "/api/rag/answers",
        json={"question": "우아한테크코스가 뭐야?"},
    )
    app.dependency_overrides.clear()

    assert response.status_code == 200
    assert response.json()["grounded"] is False
    assert response.json()["answer"] == "제공된 문서에는 서비스의 목적과 운영 방식이 없습니다."


def test_model_supported_verdict_is_removed_from_visible_answer() -> None:
    model = FakeLanguageModel(
        answer="판정: SUPPORTED\n**2026-2학기** 정정 기간은 9월 1일부터입니다."
    )
    override_service([search_result()], model)

    response = TestClient(app).post(
        "/api/rag/answers",
        json={"question": "수강신청 정정 기간은 언제야?"},
    )
    app.dependency_overrides.clear()

    assert response.status_code == 200
    assert response.json()["grounded"] is True
    assert response.json()["answer"].startswith("**2026-2학기**")
    assert "판정:" not in response.json()["answer"]


def test_rewrites_query_with_contextual_synonyms_before_search() -> None:
    model = FakeLanguageModel(rewrite="장학금 공지 장학생 모집 안내")
    search_service, _ = override_service([search_result()], model)

    response = TestClient(app).post(
        "/api/rag/answers",
        json={"question": "장학금 공고 어디서 봐?"},
    )
    app.dependency_overrides.clear()

    assert response.status_code == 200
    assert "장학금 공고 어디서 봐?" in search_service.request.query
    assert "장학금 공지 장학생 모집 안내" in search_service.request.query
    assert len(model.calls) == 2
