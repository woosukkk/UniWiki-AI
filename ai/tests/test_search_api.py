import numpy as np
from fastapi.testclient import TestClient

from app.main import app, get_search_service
from app.models import SemanticSearchRequest, SemanticSearchResult
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


def test_hybrid_ranking_prefers_exact_title_and_deduplicates_documents() -> None:
    class HybridVectorStore(FakeVectorStore):
        def search(self, query_embedding, top_k, category_id=None):
            return [
                SemanticSearchResult(chunkId="wrong-0", wikiPostId=8, title="푸른등대 장학사업",
                                     content="기부 장학생 안내", categoryId=6, chunkIndex=0, score=0.8),
                SemanticSearchResult(chunkId="wrong-1", wikiPostId=8, title="푸른등대 장학사업",
                                     content="신청 조건", categoryId=6, chunkIndex=1, score=0.79),
            ]

        def all_records(self, category_id=None):
            return [
                SemanticSearchResult(chunkId="exact-0", wikiPostId=9, title="교내장학금",
                                     content="교내 장학금 종류와 선발 조건", categoryId=6, chunkIndex=0, score=0.0)
            ]

    service = SemanticSearchService(FakeEmbedder(), HybridVectorStore(), default_top_k=5)
    results = service.search(SemanticSearchRequest(
        query="교내장학금 종류와 조건 알려줘", topK=5
    )).results

    assert results[0].wiki_post_id == 9
    assert len([result for result in results if result.wiki_post_id == 8]) == 1


def test_lexical_score_removes_korean_particles_for_title_matching() -> None:
    result = SemanticSearchResult(
        chunkId="course-guide-0",
        wikiPostId=10,
        title="2026-2 수강편람 및 강의시간표",
        content="수강신청 일정과 개설 강좌를 확인합니다.",
        categoryId=2,
        chunkIndex=0,
        score=0.0,
    )

    score = SemanticSearchService._lexical_score(
        "2026-2학기 수강편람과 수강신청 방법을 알려주세요.", result
    )

    assert score >= 0.5


def test_exact_korean_title_keyword_beats_semantically_similar_document() -> None:
    class CourseVectorStore(FakeVectorStore):
        def search(self, query_embedding, top_k, category_id=None):
            return [
                SemanticSearchResult(
                    chunkId="leave-0", wikiPostId=11,
                    title="2026학년도 2학기 휴학·복학 추가 신청 안내",
                    content="수강신청과 등록금 납부를 모두 완료해야 합니다.",
                    categoryId=2, chunkIndex=0, score=0.9,
                )
            ]

        def all_records(self, category_id=None):
            return [
                SemanticSearchResult(
                    chunkId="course-0", wikiPostId=12,
                    title="2026-2 수강편람 및 강의시간표",
                    content="수강신청 일정과 개설 강좌를 확인합니다.",
                    categoryId=2, chunkIndex=0, score=0.0,
                )
            ]

    service = SemanticSearchService(FakeEmbedder(), CourseVectorStore(), default_top_k=5)
    results = service.search(SemanticSearchRequest(
        query="2026-2학기 수강편람과 수강신청 방법을 알려주세요.", topK=5
    )).results

    assert results[0].wiki_post_id == 12


def test_expands_casual_graduation_question_with_catalog_terms() -> None:
    expanded = SemanticSearchService._expand_query(
        "소프트웨어학과 졸업하려면 뭐 필요해?"
    )

    assert "졸업 이수 학점" in expanded
    assert "수강편람" in expanded
    assert "교과과정" in expanded


def test_does_not_strip_department_suffix_as_particle() -> None:
    assert SemanticSearchService._strip_korean_particle("소프트웨어학과") == "소프트웨어학과"
    assert SemanticSearchService._strip_korean_particle("수강편람과") == "수강편람"


def test_casual_words_do_not_dilute_graduation_title_match() -> None:
    graduation = SemanticSearchResult(
        chunkId="graduation-0", wikiPostId=20,
        title="2026 소프트웨어학과 졸업 이수학점 안내",
        content="수강편람 기준 전공필수와 교양필수 학점을 안내합니다.",
        categoryId=2, chunkIndex=0, score=0.0,
    )
    internship = SemanticSearchResult(
        chunkId="internship-0", wikiPostId=21,
        title="현장실습 학점 인정 안내",
        content="졸업 전 참여할 수 있는 현장실습의 신청 방법을 안내합니다.",
        categoryId=2, chunkIndex=0, score=0.0,
    )

    expanded = SemanticSearchService._expand_query(
        "소프트웨어학과 졸업하려면 뭐 필요해?"
    )
    graduation_score = SemanticSearchService._lexical_score(
        expanded,
        graduation,
    )
    internship_score = SemanticSearchService._lexical_score(expanded, internship)

    assert graduation_score > internship_score


def test_graduation_intent_title_outranks_semantically_related_document() -> None:
    graduation = SemanticSearchResult(
        chunkId="graduation-0", wikiPostId=20,
        title="2026 소프트웨어학과 졸업 이수학점 안내",
        content="수강편람 기준 졸업 이수학점을 안내합니다.",
        categoryId=2, chunkIndex=0, score=0.35,
    )
    internship = SemanticSearchResult(
        chunkId="internship-0", wikiPostId=21,
        title="현장실습 학점 인정 안내",
        content="졸업 전 현장실습 참여 방법을 안내합니다.",
        categoryId=2, chunkIndex=0, score=0.99,
    )
    service = SemanticSearchService(FakeEmbedder(), FakeVectorStore(), 5)
    query = service._expand_query("소프트웨어학과 졸업하려면 뭐 필요해?")

    results = service._rerank(query, [internship, graduation], [], 5)

    assert results[0].wiki_post_id == 20
