import re
from datetime import date
from difflib import SequenceMatcher

from app.embedder import Embedder
from app.models import (
    SemanticSearchRequest,
    SemanticSearchResponse,
)
from app.vector_store import VectorStore


class SemanticSearchService:
    KOREAN_PARTICLE_SUFFIXES = (
        "에서부터", "으로부터", "에게서", "까지", "부터", "처럼", "보다",
        "에게", "한테", "께서", "으로", "에서", "와", "과", "은", "는",
        "이", "가", "을", "를", "의", "도", "만", "로",
    )
    def __init__(
        self,
        embedder: Embedder,
        vector_store: VectorStore,
        default_top_k: int,
        community_category_id: int = 0,
    ) -> None:
        self.embedder = embedder
        self.vector_store = vector_store
        self.default_top_k = default_top_k
        self.community_category_id = community_category_id

    def search(self, request: SemanticSearchRequest) -> SemanticSearchResponse:
        top_k = request.top_k or self.default_top_k
        expanded_query = self._expand_query(request.query)
        query_text = f"질문: {expanded_query}"
        query_vectors = self.embedder.encode([query_text])
        if len(query_vectors) != 1:
            raise RuntimeError("검색 질문 임베딩 생성에 실패했습니다.")

        vector_results = self.vector_store.search(
            query_embedding=query_vectors[0].tolist(),
            top_k=60,
            category_id=request.category_id,
        )
        keyword_results = self.vector_store.keyword_records(
            self._lexical_tokens(expanded_query),
            request.category_id,
            limit=40,
        )
        results = self._rerank(
            expanded_query,
            vector_results + keyword_results,
            max(top_k * 6, 30),
        )
        guide_terms = self._canonical_search_terms(request.query)
        if guide_terms:
            guide_query = " ".join(guide_terms)
            guide_vectors = self.embedder.encode([f"질문: {guide_query}"])
            guide_vector_results = self.vector_store.search(
                query_embedding=guide_vectors[0].tolist(),
                top_k=30,
                category_id=request.category_id,
            )
            guide_results = self.vector_store.keyword_records(
                guide_terms,
                request.category_id,
                limit=30,
            )
            results = self._rerank(
                expanded_query,
                vector_results + keyword_results + guide_vector_results + guide_results,
                max(top_k * 6, 30),
            )
        results = self._prefer_current_period(request.query, results)
        results = results[:top_k]
        return SemanticSearchResponse(
            query=request.query,
            topK=top_k,
            resultCount=len(results),
            results=results,
        )

    @staticmethod
    def _expand_query(query):
        expanded_terms = []
        normalized = re.sub(r"\s+", "", query.lower())
        if re.search(r"졸업(?!생)|졸업하려|졸업조건|졸업기준", normalized):
            expanded_terms.extend([
                "졸업 요건",
                "졸업 이수 학점",
                "전공필수",
                "교양필수",
                "수강편람",
                "교과과정",
            ])
            if re.search(r"소프트웨어(?:학과|과)?|소융대", normalized):
                expanded_terms.extend(["소프트웨어학과", "소프트웨어학과 졸업 이수학점"])
        if re.search(r"교내장학|학교장학|장학금(?:종류|제도|혜택)|장학.*(?:종류|어떤|뭐)", normalized):
            expanded_terms.extend([
                "교내 장학금",
                "재학생 장학금 신청 기본 안내",
                "장학금 신청 중복수혜 기본 원칙",
                "성적우수 에델바이스 자체 선발",
            ])
        if re.search(r"수강신청|과목신청|강의신청", normalized):
            expanded_terms.extend(["수강신청", "수강편람", "강의시간표"])
        if not expanded_terms:
            return query
        return f"{query} {' '.join(dict.fromkeys(expanded_terms))}"

    def _rerank(self, query, vector_results, top_k):
        vector_scores = {}
        for result in vector_results:
            vector_scores[result.chunk_id] = max(
                vector_scores.get(result.chunk_id, 0.0),
                result.score,
            )
        candidates = {result.chunk_id: result for result in vector_results}

        ranked = []
        for result in candidates.values():
            lexical = self._lexical_score(query, result)
            vector = max(0.0, vector_scores.get(result.chunk_id, 0.0))
            score = min(1.0, vector * 0.3 + lexical * 0.8)
            score += self._intent_title_boost(query, result)
            score += self._source_priority_boost(query, result)
            score += self._document_role_adjustment(query, result)
            score = max(0.0, min(1.0, score))
            ranked.append(result.model_copy(update={"score": score}))
        ranked.sort(
            key=lambda result: (
                self._document_role_rank(query, result),
                result.score,
                min(len(result.content), 800),
            ),
            reverse=True,
        )

        deduplicated = []
        seen_wiki_posts = set()
        seen_titles = []
        for result in ranked:
            normalized_title = self._canonical_title(result.title)
            if (result.wiki_post_id in seen_wiki_posts
                    or any(self._titles_are_duplicates(normalized_title, seen)
                           for seen in seen_titles)):
                continue
            seen_wiki_posts.add(result.wiki_post_id)
            seen_titles.append(normalized_title)
            deduplicated.append(result)
            if len(deduplicated) >= top_k:
                break
        return deduplicated

    def _source_priority_boost(self, query, result):
        is_community = result.category_id == self.community_category_id
        normalized = re.sub(r"\s+", "", query.lower())
        official_intent = re.search(
            r"일정|기간|날짜|언제|규정|규칙|장학금|졸업|수강|학점|등록금|휴학|복학|신청|마감",
            normalized,
        )
        if official_intent:
            return 0.25 if not is_community else 0.0
        community_intent = re.search(
            r"후기|팁|경험|추천|취업|프로젝트|면접|코딩테스트|인턴|동아리|공부법|노하우",
            normalized,
        )
        if community_intent:
            return 0.20 if is_community else 0.0
        return 0.05 if is_community else 0.0

    @classmethod
    def _document_role_adjustment(cls, query, result):
        normalized = re.sub(r"\s+", "", query.lower())
        role = result.document_type
        if role == "GENERAL":
            compact_title = re.sub(r"\s+", "", result.title)
            if any(term in compact_title for term in (
                "기본안내", "기본원칙", "이수학점안내", "적용기준",
                "수강편람", "학사규정", "장학제도", "교내장학금",
            )):
                role = "CANONICAL_GUIDE"
            elif any(term in compact_title for term in (
                "신청안내", "선발안내", "선발결과", "지급안내", "모집안내",
            )):
                role = "OFFICIAL_NOTICE"

        guide_intent = re.search(
            r"요건|조건|기준|원칙|종류|전체|총몇|몇학점|방법|어떤|뭐가",
            normalized,
        )
        period_intent = re.search(r"기간|일정|날짜|언제|마감|접수", normalized)
        if guide_intent:
            if role == "CANONICAL_GUIDE":
                return 0.45
            if role == "OFFICIAL_NOTICE":
                return -0.12
        if period_intent and role == "OFFICIAL_NOTICE":
            return 0.30
        return 0.0

    @classmethod
    def _document_role_rank(cls, query, result):
        adjustment = cls._document_role_adjustment(query, result)
        if adjustment > 0:
            return 2
        if adjustment < 0:
            return 0
        return 1

    @staticmethod
    def _canonical_search_terms(query):
        normalized = re.sub(r"\s+", "", query.lower())
        if re.search(r"졸업(?!생)|졸업조건|졸업기준|졸업요건|졸업학점", normalized):
            terms = ["졸업 이수학점", "이수학점 안내", "전공필수", "수강편람"]
            if re.search(r"소프트웨어(?:학과|과)?|소융대", normalized):
                terms.insert(0, "소프트웨어학과 졸업")
            return terms
        if re.search(r"교내장학|학교장학|장학금", normalized):
            return [
                "재학생 장학금 신청 기본 안내",
                "장학금 신청과 중복수혜 기본 원칙",
                "교내장학금",
                "자체 선발",
            ]
        return []

    @staticmethod
    def _canonical_title(title):
        return re.sub(r"[^0-9a-z가-힣]", "", title.lower())

    @staticmethod
    def _titles_are_duplicates(left, right):
        if left == right:
            return True
        if min(len(left), len(right)) >= 24 and (
                left[:24] == right[:24]
                or SequenceMatcher(None, left, right).ratio() >= 0.92):
            return True
        return False

    @staticmethod
    def _prefer_current_period(query, results):
        """Avoid mixing several academic periods when the user omitted one."""
        if re.search(r"20\d{2}", query):
            return results
        if not re.search(r"기간|일정|날짜|언제|정정|마감|접수", query):
            return results

        dated = []
        for result in results:
            match = re.search(r"(20\d{2})(?:\s*[-년]\s*([12])(?:학기)?)?", result.title)
            if match:
                dated.append((result, int(match.group(1)), int(match.group(2) or 0)))
        if not dated:
            return results

        current = date.today()
        current_term = 1 if current.month < 7 else 2
        available_years = [year for _, year, _ in dated if year <= current.year]
        target_year = max(available_years, default=max(year for _, year, _ in dated))
        available_terms = [
            term for _, year, term in dated
            if year == target_year and term and (year < current.year or term <= current_term)
        ]
        target_term = max(available_terms, default=0)

        scoped = [
            result for result, year, term in dated
            if year == target_year and (not target_term or term in (0, target_term))
        ]
        return scoped or results

    def expand_results(self, results, max_chunks_per_document=8):
        if not results:
            return results
        records_by_wiki_post = {}
        records = self.vector_store.records_for_wiki_posts(
            [result.wiki_post_id for result in results]
        )
        for record in records:
            records_by_wiki_post.setdefault(record.wiki_post_id, []).append(record)

        expanded = []
        for result in results:
            records = records_by_wiki_post.get(result.wiki_post_id, [result])
            records.sort(key=lambda record: record.chunk_index)
            for record in records[:max_chunks_per_document]:
                expanded.append(record.model_copy(update={"score": result.score}))
        return expanded

    @staticmethod
    def _lexical_score(query, result):
        tokens = SemanticSearchService._lexical_tokens(query)
        if not tokens:
            return 0.0
        title = re.sub(r"\s+", "", result.title.lower())
        content = re.sub(r"\s+", "", result.content.lower())
        compact_query = re.sub(r"\s+", "", query.lower())
        points = 6.0 if len(title) >= 2 and title in compact_query else 0.0
        for token in tokens:
            compact = token.replace(" ", "")
            if compact in title:
                points += (
                    12.0
                    if len(compact) >= 4 and re.search(r"[가-힣]", compact)
                    else 8.0
                    if len(compact) >= 3 and re.search(r"[가-힣]", compact)
                    else 4.0
                )
            elif len(title) >= 2 and title in compact:
                points += 3.0
            elif any(compact[index:index + 3] in title for index in range(max(0, len(compact) - 2))):
                points += 1.5
            if compact in content:
                points += 1.0
        query_years = set(re.findall(r"20\d{2}", query))
        if query_years and any(year in title for year in query_years):
            points += 3.0
        return min(1.0, points / (len(tokens) * 4.0 + 3.0))

    @staticmethod
    def _lexical_tokens(query):
        tokens = [
            SemanticSearchService._strip_korean_particle(token)
            for token in re.findall(r"[0-9A-Za-z가-힣]+", query.lower())
            if token not in {
                "알려줘", "알려주세요", "어디서", "어디", "확인", "관련", "정보",
                "뭐야", "뭐", "필요해", "필요한", "어떻게", "돼", "되나요", "하려면",
            }
            and not token.endswith("하려면")
        ]
        compounds = [
            left + right
            for left, right in zip(tokens, tokens[1:])
            if len(left + right) >= 4
        ]
        prioritized = sorted(compounds, key=len, reverse=True) + tokens
        return list(dict.fromkeys(prioritized))

    @staticmethod
    def _intent_title_boost(query, result):
        normalized_query = re.sub(r"\s+", "", query.lower())
        normalized_title = re.sub(r"\s+", "", result.title.lower())
        boost = 0.0
        exact_title_terms = [
            token.replace(" ", "")
            for token in SemanticSearchService._lexical_tokens(query)
            if len(token.replace(" ", "")) >= 3
        ]
        if any(term in normalized_title for term in exact_title_terms):
            boost += 0.35
        graduation_intent = re.search(
            r"졸업(?!생)|졸업하려|졸업조건|졸업기준",
            normalized_query,
        )
        if graduation_intent and "졸업" in normalized_title:
            boost += 0.25
        departments = set(re.findall(r"[가-힣]+학과", normalized_query))
        if departments and any(department in normalized_title for department in departments):
            boost += 0.15
        return boost

    @staticmethod
    def _strip_korean_particle(token):
        for suffix in SemanticSearchService.KOREAN_PARTICLE_SUFFIXES:
            if suffix == "과" and token.endswith(("학과", "교과")):
                continue
            if token.endswith(suffix) and len(token) - len(suffix) >= 2:
                return token[:-len(suffix)]
        return token
