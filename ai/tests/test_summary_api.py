from fastapi.testclient import TestClient

from app.llm import LlmProviderError
from app.main import app, get_summary_service
from app.models import (
    ChunkMetadata,
    VectorStoreDocumentResponse,
    VectorStoreRecord,
)
from app.summary_service import WikiSummaryService


class FakeVectorStore:
    collection_name = "test"

    def __init__(self, chunks):
        self.chunks = chunks

    def get_document(self, wiki_post_id):
        return VectorStoreDocumentResponse(
            wikiPostId=wiki_post_id,
            chunkCount=len(self.chunks),
            chunks=self.chunks,
        )


class FakeLanguageModel:
    def __init__(self, answers=None, error=None):
        self.answers = list(answers or ["핵심 요약"])
        self.error = error
        self.calls = []

    def generate(self, instructions, input_text):
        self.calls.append((instructions, input_text))
        if self.error:
            raise self.error
        return self.answers.pop(0)


def chunk(index, text):
    return VectorStoreRecord(
        chunkId=f"wiki-7-chunk-{index}",
        text=text,
        embedding=[1.0, 0.0],
        metadata=ChunkMetadata(
            wikiPostId=7,
            title="수강신청 안내",
            categoryId=1,
            chunkIndex=index,
        ),
    )


def override_service(chunks, language_model=None, context_chars=4000):
    language_model = language_model or FakeLanguageModel()
    service = WikiSummaryService(
        vector_store=FakeVectorStore(chunks),
        language_model=language_model,
        default_max_chars=600,
        context_chars=context_chars,
    )
    app.dependency_overrides[get_summary_service] = lambda: service
    return language_model


def test_summarizes_wiki_document_by_id() -> None:
    language_model = override_service([chunk(0, "수강신청은 학교 포털에서 진행합니다.")])
    response = TestClient(app).post(
        "/api/summaries/wiki-posts/7",
        params={"maxChars": 300},
    )
    app.dependency_overrides.clear()

    assert response.status_code == 200
    assert response.json() == {
        "wikiPostId": 7,
        "title": "수강신청 안내",
        "summary": "핵심 요약",
        "sourceChunkCount": 1,
    }
    assert "300자 이내" in language_model.calls[0][1]


def test_summarizes_long_document_in_multiple_stages() -> None:
    language_model = FakeLanguageModel(answers=["1차 요약", "2차 요약", "최종 요약"])
    override_service(
        [chunk(0, "가" * 2500), chunk(1, "나" * 2500)],
        language_model,
        context_chars=4000,
    )
    response = TestClient(app).post("/api/summaries/wiki-posts/7")
    app.dependency_overrides.clear()

    assert response.status_code == 200
    assert response.json()["summary"] == "최종 요약"
    assert response.json()["sourceChunkCount"] == 2
    assert len(language_model.calls) == 3


def test_returns_not_found_for_empty_document() -> None:
    language_model = override_service([])
    response = TestClient(app).post("/api/summaries/wiki-posts/99")
    app.dependency_overrides.clear()

    assert response.status_code == 404
    assert language_model.calls == []


def test_returns_bad_gateway_when_summary_generation_fails() -> None:
    override_service(
        [chunk(0, "내용")],
        FakeLanguageModel(error=LlmProviderError("failed")),
    )
    response = TestClient(app).post("/api/summaries/wiki-posts/7")
    app.dependency_overrides.clear()

    assert response.status_code == 502


def test_rejects_invalid_summary_length() -> None:
    response = TestClient(app).post(
        "/api/summaries/wiki-posts/7",
        params={"maxChars": 50},
    )
    assert response.status_code == 422
