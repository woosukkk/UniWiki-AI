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
    def __init__(self, answer="포털의 수강신청 메뉴에서 신청할 수 있습니다.", error=None):
        self.answer = answer
        self.error = error
        self.calls = []

    def generate(self, instructions, input_text):
        self.calls.append((instructions, input_text))
        if self.error:
            raise self.error
        return self.answer


def search_result(score=0.91):
    return SemanticSearchResult(chunkId="wiki-7-chunk-0", wikiPostId=7, title="수강신청 안내", content="수강신청은 학교 포털의 수강신청 메뉴에서 진행합니다.", categoryId=2, chunkIndex=0, score=score)


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
    assert response.json() == {"question": "수강신청은 어디서 하나요?", "answer": "포털의 수강신청 메뉴에서 신청할 수 있습니다.", "grounded": True, "retrievedChunkCount": 1}
    assert search_service.request.top_k == 4
    assert search_service.request.category_id == 2
    assert "수강신청 안내" in language_model.calls[0][1]


def test_does_not_call_llm_when_evidence_is_insufficient() -> None:
    _, language_model = override_service([search_result(score=0.2)])
    response = TestClient(app).post("/api/rag/answers", json={"question": "등록금은 얼마인가요?"})
    app.dependency_overrides.clear()
    assert response.status_code == 200
    assert response.json()["answer"] == INSUFFICIENT_EVIDENCE_ANSWER
    assert response.json()["grounded"] is False
    assert language_model.calls == []


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
