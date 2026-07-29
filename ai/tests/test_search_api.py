import numpy as np
from fastapi.testclient import TestClient

from app.main import app, get_search_service
from app.models import SemanticSearchResult
from app.search_service import SemanticSearchService


class FakeEmbedder:
    model_name = "test-model"

    def encode(self, texts):
        return np.array([[1.0, 0.0, 0.0]], dtype=np.float32)


class FakeVectorStore:
    collection_name = "test"

    def __init__(self):
        self.search_arguments = None

    def search(self, query_embedding, top_k, category_id=None):
        self.search_arguments = (query_embedding, top_k, category_id)
        return [
            SemanticSearchResult(
                chunkId="wiki-7-chunk-0",
                wikiPostId=7,
                title="수강신청 안내",
                content="수강신청은 학교 포털에서 진행합니다.",
                categoryId=2,
                chunkIndex=0,
                score=0.91,
            )
        ]


def test_searches_wiki_chunks_with_top_k_and_metadata() -> None:
    vector_store = FakeVectorStore()
    service = SemanticSearchService(FakeEmbedder(), vector_store, default_top_k=5)
    app.dependency_overrides[get_search_service] = lambda: service
    client = TestClient(app)

    response = client.post(
        "/api/search/wiki-posts",
        json={"query": "수강신청은 어디서 하나요?", "topK": 3, "categoryId": 2},
    )

    app.dependency_overrides.clear()
    assert response.status_code == 200
    body = response.json()
    assert body["query"] == "수강신청은 어디서 하나요?"
    assert body["topK"] == 3
    assert body["resultCount"] == 1
    assert body["results"][0] == {
        "chunkId": "wiki-7-chunk-0",
        "wikiPostId": 7,
        "title": "수강신청 안내",
        "content": "수강신청은 학교 포털에서 진행합니다.",
        "categoryId": 2,
        "chunkIndex": 0,
        "score": 0.91,
    }
    assert vector_store.search_arguments == ([1.0, 0.0, 0.0], 3, 2)


def test_rejects_blank_search_query() -> None:
    client = TestClient(app)
    response = client.post("/api/search/wiki-posts", json={"query": "   "})

    assert response.status_code == 422
