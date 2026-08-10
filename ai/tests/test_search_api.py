import numpy as np
from datetime import date
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

    def records_for_wiki_posts(self, wiki_post_ids):
        return []

    def records_for_categories(self, category_ids):
        return []

    def records_for_titles(self, titles):
        return []

    def keyword_records(self, terms, category_id=None, limit=20):
        return []


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
    assert body["results"][0] | {"score": 0.91} == {
        "chunkId": "wiki-7-chunk-0",
        "wikiPostId": 7,
        "title": "수강신청 안내",
        "content": "수강신청은 학교 포털에서 진행합니다.",
        "categoryId": 2,
        "chunkIndex": 0,
        "documentType": "GENERAL",
        "score": 0.91,
    }
    assert vector_store.search_arguments == ([1.0, 0.0, 0.0], 60, 2)


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

        def keyword_records(self, terms, category_id=None, limit=20):
            return [
                SemanticSearchResult(chunkId="exact-0", wikiPostId=9, title="교내장학금",
                                     content="교내 장학금 종류와 선발 조건", categoryId=6, chunkIndex=0, score=0.0),
            ]

    service = SemanticSearchService(FakeEmbedder(), HybridVectorStore(), default_top_k=5)
    results = service.search(SemanticSearchRequest(
        query="교내장학금 종류와 조건 알려줘", topK=5
    )).results

    assert results[0].wiki_post_id == 9
    assert len([result for result in results if result.wiki_post_id == 8]) == 1


def test_spaced_korean_title_adds_compound_keyword_candidate() -> None:
    tokens = SemanticSearchService._lexical_tokens("교내 장학금 종류를 알려줘")
    assert "교내장학금" in tokens
    assert tokens.index("교내장학금") < tokens.index("장학금")


def test_expands_school_scholarship_and_software_department_aliases() -> None:
    scholarship = SemanticSearchService._expand_query("학교에서 주는 장학금 종류가 뭐야?")
    graduation = SemanticSearchService._expand_query("소프트웨어 졸업학점 총 몇 학점이야?")

    assert scholarship == "학교에서 주는 장학금 종류가 뭐야?"
    assert graduation == "소프트웨어 졸업학점 총 몇 학점이야?"


def test_searches_every_document_in_the_best_matching_category() -> None:
    class CategoryStore(FakeVectorStore):
        requested_categories = None

        def search(self, query_embedding, top_k, category_id=None):
            return [SemanticSearchResult(
                chunkId="seed-0", wikiPostId=80, title="학사 안내",
                content="학교 생활 안내", categoryId=6, chunkIndex=0, score=0.9,
            )]

        def records_for_categories(self, category_ids):
            self.requested_categories = category_ids
            return [SemanticSearchResult(
                chunkId="answer-0", wikiPostId=60, title="교내 장학금 종류",
                content="교내 장학금 종류와 신청 조건", categoryId=6,
                chunkIndex=0, score=0.0,
            )]

    store = CategoryStore()
    results = SemanticSearchService(FakeEmbedder(), store, 5).search(
        SemanticSearchRequest(query="교내 장학금 종류", topK=5)
    ).results

    assert store.requested_categories == [6]
    assert results[0].wiki_post_id == 60


def test_explicit_category_searches_only_that_category() -> None:
    class CategoryStore(FakeVectorStore):
        requested_categories = None

        def records_for_categories(self, category_ids):
            self.requested_categories = category_ids
            return []

    store = CategoryStore()
    SemanticSearchService(FakeEmbedder(), store, 5).search(
        SemanticSearchRequest(query="등록금", topK=5, categoryId=9)
    )

    assert store.requested_categories == [9]


def test_scholarship_question_always_keeps_campus_scholarship_guide() -> None:
    class ScholarshipStore(FakeVectorStore):
        requested_titles = None

        def records_for_titles(self, titles):
            self.requested_titles = titles
            return [SemanticSearchResult(
                chunkId="scholarship-0", wikiPostId=8884, title="교내장학금",
                content="교내 장학금 종류와 신청 조건", categoryId=176,
                chunkIndex=0, score=0.0,
            )]

    store = ScholarshipStore()
    results = SemanticSearchService(FakeEmbedder(), store, 5).search(
        SemanticSearchRequest(query="장학금 알려줘", topK=5)
    ).results

    assert store.requested_titles == ["교내장학금"]
    assert any(result.wiki_post_id == 8884 for result in results)


def test_searches_at_most_five_matching_categories_with_seventy_keywords() -> None:
    class CategoryStore(FakeVectorStore):
        requested_categories = None
        keyword_limit = None

        def search(self, query_embedding, top_k, category_id=None):
            return [
                SemanticSearchResult(
                    chunkId=f"seed-{category_id}", wikiPostId=category_id,
                    title=f"카테고리 {category_id}", content="검색 대상",
                    categoryId=category_id, chunkIndex=0, score=1.0,
                )
                for category_id in range(1, 7)
            ]

        def keyword_records(self, terms, category_id=None, limit=20):
            self.keyword_limit = limit
            return []

        def records_for_categories(self, category_ids):
            self.requested_categories = category_ids
            return []

    store = CategoryStore()
    SemanticSearchService(FakeEmbedder(), store, 5).search(
        SemanticSearchRequest(query="검색 대상", topK=5)
    )

    assert store.keyword_limit == 70
    assert store.requested_categories == [1, 2, 3, 4, 5]


def test_source_priority_follows_question_intent() -> None:
    service = SemanticSearchService(
        FakeEmbedder(), FakeVectorStore(), 5, community_category_id=304
    )
    official = SemanticSearchResult(
        chunkId="official-0", wikiPostId=1, title="공식 자료", content="본문",
        categoryId=2, chunkIndex=0, score=0.0,
    )
    community = SemanticSearchResult(
        chunkId="community-0", wikiPostId=2, title="함께 만든 위키", content="본문",
        categoryId=304, chunkIndex=0, score=0.0,
    )

    assert service._source_priority_boost("프로젝트 후기 알려줘", community) == 0.20
    assert service._source_priority_boost("장학금 신청 기간", official) == 0.25
    assert service._source_priority_boost("장학금 신청 후기", community) == 0.0
    assert service._source_priority_boost("학교생활 알려줘", community) == 0.05


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


def test_exact_proper_noun_title_is_not_diluted_by_rewrite_terms() -> None:
    exact = SemanticSearchResult(
        chunkId="woowacourse-0", wikiPostId=13,
        title="우아한테크코스 멘토링", content="졸업생 멘토링 안내",
        categoryId=2, chunkIndex=0, score=0.0,
    )

    boost = SemanticSearchService._intent_title_boost(
        "우아한테크코스가 뭐야 개발자 교육 과정 취업 멘토링 프로그램",
        exact,
    )

    assert boost == 0.35


def test_expands_casual_graduation_question_with_catalog_terms() -> None:
    expanded = SemanticSearchService._expand_query(
        "소프트웨어학과 졸업하려면 뭐 필요해?"
    )

    assert expanded == "소프트웨어학과 졸업하려면 뭐 필요해?"


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

    results = service._rerank(query, [internship, graduation], 5)

    assert results[0].wiki_post_id == 20


def test_alumni_contact_question_does_not_expand_to_graduation_requirements() -> None:
    expanded = SemanticSearchService._expand_query("졸업생과 연락할 수단이 있어?")

    assert expanded == "졸업생과 연락할 수단이 있어?"
    assert "졸업 이수 학점" not in expanded
    assert "수강편람" not in expanded


def test_lexical_match_uses_same_hybrid_scale_for_every_source() -> None:
    community = SemanticSearchResult(
        chunkId="community-0", wikiPostId=1377,
        title="세종대학교 졸업생과 취업 준비 후배 연결 서비스",
        content="재직 중인 졸업생을 찾아 게시판으로 질문할 수 있는 서비스입니다.",
        categoryId=2, chunkIndex=0, score=0.35,
    )
    graduation = SemanticSearchResult(
        chunkId="graduation-0", wikiPostId=47,
        title="2026 소프트웨어학과 졸업 이수학점 안내",
        content="졸업에 필요한 전공필수와 교양필수 학점을 안내합니다.",
        categoryId=2, chunkIndex=0, score=0.99,
    )
    service = SemanticSearchService(FakeEmbedder(), FakeVectorStore(), 5)
    query = service._expand_query("졸업생과 연락할 수단이 있어?")

    results = service._rerank(query, [graduation, community], 5)

    assert service._lexical_score(query, community) >= 0.33
    assert results[0].wiki_post_id == 1377


def test_period_question_without_year_uses_current_academic_period() -> None:
    current_year = date.today().year
    current_term = 1 if date.today().month < 7 else 2
    other_year = current_year - 2
    results = [
        SemanticSearchResult(
            chunkId="old-0", wikiPostId=30,
            title=f"{other_year}-2 수강편람 및 강의시간표",
            content="수강신청 정정 기간 안내", categoryId=2,
            chunkIndex=0, score=0.8,
        ),
        SemanticSearchResult(
            chunkId="current-0", wikiPostId=31,
            title=f"{current_year}-{current_term} 수강편람 및 강의시간표",
            content="수강신청 정정 기간 안내", categoryId=2,
            chunkIndex=0, score=0.8,
        ),
    ]

    scoped = SemanticSearchService._prefer_current_period(
        "수강신청 정정 기간은 언제야?", results,
    )

    assert [result.wiki_post_id for result in scoped] == [31]


def test_explicit_year_keeps_requested_period_results() -> None:
    results = [
        SemanticSearchResult(
            chunkId="2024-0", wikiPostId=40,
            title="2024-2 수강편람", content="정정 기간", categoryId=2,
            chunkIndex=0, score=0.8,
        ),
        SemanticSearchResult(
            chunkId="2026-0", wikiPostId=41,
            title="2026-1 수강편람", content="정정 기간", categoryId=2,
            chunkIndex=0, score=0.8,
        ),
    ]

    assert SemanticSearchService._prefer_current_period(
        "2024-2 수강신청 정정 기간", results,
    ) == results


def test_nearly_identical_titles_are_deduplicated() -> None:
    service = SemanticSearchService(FakeEmbedder(), FakeVectorStore(), 5)
    first = SemanticSearchResult(
        chunkId="community-1", wikiPostId=50,
        title="안녕하세요! 현재 우아한테크코스에서 세종대학교 졸업생을 찾습니다",
        content="졸업생과 취업 준비생 연결", categoryId=2,
        chunkIndex=0, score=0.9,
    )
    duplicate = SemanticSearchResult(
        chunkId="community-2", wikiPostId=51,
        title="안녕하세요 현재 우아한테크코스에서 세종대학교 졸업생을 찾습니다.",
        content="졸업생과 취업 준비생 연결", categoryId=2,
        chunkIndex=0, score=0.89,
    )

    results = service._rerank("우아한테크코스가 뭐야", [first, duplicate], 5)

    assert len(results) == 1


def test_expands_only_selected_wiki_documents() -> None:
    class SelectedDocumentStore(FakeVectorStore):
        def __init__(self):
            super().__init__()
            self.requested_ids = None

        def records_for_wiki_posts(self, wiki_post_ids):
            self.requested_ids = wiki_post_ids
            return [
                SemanticSearchResult(
                    chunkId=f"wiki-7-chunk-{index}", wikiPostId=7,
                    title="수강신청 안내", content=f"청크 {index}",
                    categoryId=2, chunkIndex=index, score=0.0,
                )
                for index in range(10)
            ]

    store = SelectedDocumentStore()
    service = SemanticSearchService(FakeEmbedder(), store, 5)
    selected = store.search([], 1)

    expanded = service.expand_results(selected)

    assert store.requested_ids == [7]
    assert len(expanded) == 3
    assert [result.chunk_index for result in expanded] == list(range(3))
