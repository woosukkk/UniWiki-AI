from app.embedder import Embedder
from app.models import (
    SemanticSearchRequest,
    SemanticSearchResponse,
)
from app.vector_store import VectorStore


class SemanticSearchService:
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

        results = self.vector_store.search(
            query_embedding=query_vectors[0].tolist(),
            top_k=top_k,
            category_id=request.category_id,
        )
        return SemanticSearchResponse(
            query=request.query,
            topK=top_k,
            resultCount=len(results),
            results=results,
        )
