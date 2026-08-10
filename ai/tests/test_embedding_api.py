import numpy as np
from fastapi.testclient import TestClient

from app.embedding_service import WikiEmbeddingService, classify_source_key
from app.main import app, get_embedding_service


class FakeEmbedder:
    model_name = "test-model"

    def encode(self, texts):
        return np.array([[float(index), 0.5, 1.0] for index, _ in enumerate(texts)])


def test_creates_chunks_embeddings_and_metadata() -> None:
    service = WikiEmbeddingService(FakeEmbedder(), max_chars=100, overlap_chars=10)
    app.dependency_overrides[get_embedding_service] = lambda: service
    client = TestClient(app)

    response = client.post(
        "/api/embeddings/wiki-posts",
        json={
            "wikiPostId": 7,
            "title": "수강신청 안내",
            "content": "첫 번째 안내입니다.\n\n두 번째 안내입니다.",
            "categoryId": 2,
        },
    )

    app.dependency_overrides.clear()
    assert response.status_code == 200
    body = response.json()
    assert body["wikiPostId"] == 7
    assert body["model"] == "test-model"
    assert body["dimension"] == 3
    assert body["chunkCount"] == 1
    assert body["chunks"][0]["chunkId"] == "wiki-7-chunk-0"
    assert body["chunks"][0]["text"].startswith(body["chunks"][0]["metadata"]["title"])
    assert body["chunks"][0]["metadata"]["documentType"] == "OFFICIAL_NOTICE"
    assert body["chunks"][0]["metadata"] == {
        "wikiPostId": 7,
        "title": "수강신청 안내",
        "categoryId": 2,
        "chunkIndex": 0,
        "documentType": "OFFICIAL_NOTICE",
        "sourceKey": "",
    }


def test_rejects_blank_document_content() -> None:
    client = TestClient(app)
    response = client.post(
        "/api/embeddings/wiki-posts",
        json={"wikiPostId": 1, "title": "제목", "content": "   ", "categoryId": 1},
    )

    assert response.status_code == 422


def test_classifies_stable_source_keys_from_titles() -> None:
    assert classify_source_key("2026 소프트웨어학과 졸업 이수학점 안내") == \
        "software-graduation-requirements"
    assert classify_source_key("세종대학교 등록금 납부와 등록 안내") == "tuition-policy"
    assert classify_source_key("교내장학금 기본 안내") == "scholarship-policy"
