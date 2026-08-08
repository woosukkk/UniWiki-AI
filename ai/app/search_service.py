import re

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
    ) -> None:
        self.embedder = embedder
        self.vector_store = vector_store
        self.default_top_k = default_top_k

    def search(self, request: SemanticSearchRequest) -> SemanticSearchResponse:
        top_k = request.top_k or self.default_top_k
        query_text = f"질문: {request.query}"
        query_vectors = self.embedder.encode([query_text])
        if len(query_vectors) != 1:
            raise RuntimeError("검색 질문 임베딩 생성에 실패했습니다.")

        supports_lexical = hasattr(self.vector_store, "all_records")
        vector_results = self.vector_store.search(
            query_embedding=query_vectors[0].tolist(),
            top_k=max(top_k * 6, 30) if supports_lexical else top_k,
            category_id=request.category_id,
        )
        lexical_records = (
            self.vector_store.all_records(request.category_id)
            if supports_lexical else []
        )
        results = (
            self._rerank(request.query, vector_results, lexical_records, top_k)
            if supports_lexical else vector_results[:top_k]
        )
        return SemanticSearchResponse(
            query=request.query,
            topK=top_k,
            resultCount=len(results),
            results=results,
        )

    def _rerank(self, query, vector_results, lexical_records, top_k):
        vector_scores = {result.chunk_id: result.score for result in vector_results}
        candidates = {result.chunk_id: result for result in vector_results}
        for result in lexical_records:
            if self._lexical_score(query, result) > 0:
                candidates[result.chunk_id] = result

        ranked = []
        for result in candidates.values():
            lexical = self._lexical_score(query, result)
            vector = max(0.0, vector_scores.get(result.chunk_id, 0.0))
            score = min(1.0, vector * 0.55 + lexical * 0.65)
            ranked.append(result.model_copy(update={"score": score}))
        ranked.sort(
            key=lambda result: (result.score, min(len(result.content), 800)),
            reverse=True,
        )

        deduplicated = []
        seen_wiki_posts = set()
        for result in ranked:
            if result.wiki_post_id in seen_wiki_posts:
                continue
            seen_wiki_posts.add(result.wiki_post_id)
            deduplicated.append(result)
            if len(deduplicated) >= top_k:
                break
        return deduplicated

    def expand_results(self, results, max_chunks_per_document=8):
        if not hasattr(self.vector_store, "all_records"):
            return results
        records_by_wiki_post = {}
        for record in self.vector_store.all_records():
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
        tokens = [
            SemanticSearchService._strip_korean_particle(token)
            for token in re.findall(r"[0-9A-Za-z가-힣]+", query.lower())
            if token not in {"알려줘", "알려주세요", "어디서", "어디", "확인", "관련", "정보", "뭐야"}
        ]
        tokens = [token for token in tokens if token]
        if not tokens:
            return 0.0
        title = re.sub(r"\s+", "", result.title.lower())
        content = re.sub(r"\s+", "", result.content.lower())
        compact_query = re.sub(r"\s+", "", query.lower())
        points = 6.0 if len(title) >= 2 and title in compact_query else 0.0
        for token in tokens:
            compact = token.replace(" ", "")
            if compact in title:
                points += 4.0
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
    def _strip_korean_particle(token):
        for suffix in SemanticSearchService.KOREAN_PARTICLE_SUFFIXES:
            if token.endswith(suffix) and len(token) - len(suffix) >= 2:
                return token[:-len(suffix)]
        return token
