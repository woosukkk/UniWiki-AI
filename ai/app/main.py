from functools import lru_cache

from fastapi import Depends, FastAPI, Response, status

from app.config import settings
from app.embedder import SentenceTransformerEmbedder
from app.embedding_service import WikiEmbeddingService
from app.models import (
    HealthResponse,
    SemanticSearchRequest,
    SemanticSearchResponse,
    VectorStoreDocumentResponse,
    VectorStoreStatsResponse,
    VectorStoreWriteResponse,
    WikiDocumentRequest,
    WikiEmbeddingResponse,
)
from app.vector_store import ChromaVectorStore
from app.vector_store_service import WikiVectorStoreService
from app.search_service import SemanticSearchService

app = FastAPI(
    title="UniWiki AI Service",
    version="0.1.0",
    description="위키 문서 청킹 및 임베딩 API",
)


@lru_cache(maxsize=1)
def get_embedding_service() -> WikiEmbeddingService:
    embedder = SentenceTransformerEmbedder(settings.embedding_model)
    return WikiEmbeddingService(
        embedder=embedder,
        max_chars=settings.chunk_max_chars,
        overlap_chars=settings.chunk_overlap_chars,
    )


@lru_cache(maxsize=1)
def get_vector_store() -> ChromaVectorStore:
    return ChromaVectorStore(
        persist_dir=settings.chroma_persist_dir,
        collection_name=settings.chroma_collection,
    )


@lru_cache(maxsize=1)
def get_vector_store_service() -> WikiVectorStoreService:
    return WikiVectorStoreService(
        embedding_service=get_embedding_service(),
        vector_store=get_vector_store(),
    )


@lru_cache(maxsize=1)
def get_search_service() -> SemanticSearchService:
    return SemanticSearchService(
        embedder=get_embedding_service().embedder,
        vector_store=get_vector_store(),
        default_top_k=settings.search_top_k,
    )


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(status="ok", model=settings.embedding_model)


@app.post(
    "/api/embeddings/wiki-posts",
    response_model=WikiEmbeddingResponse,
    response_model_by_alias=True,
)
def create_wiki_embeddings(
    document: WikiDocumentRequest,
    service: WikiEmbeddingService = Depends(get_embedding_service),
) -> WikiEmbeddingResponse:
    return service.embed(document)


@app.put(
    "/api/vector-store/wiki-posts",
    response_model=VectorStoreWriteResponse,
    response_model_by_alias=True,
)
def save_wiki_document(
    document: WikiDocumentRequest,
    service: WikiVectorStoreService = Depends(get_vector_store_service),
) -> VectorStoreWriteResponse:
    return service.save(document)


@app.get(
    "/api/vector-store/wiki-posts/{wiki_post_id}",
    response_model=VectorStoreDocumentResponse,
    response_model_by_alias=True,
)
def get_wiki_document(
    wiki_post_id: int,
    service: WikiVectorStoreService = Depends(get_vector_store_service),
) -> VectorStoreDocumentResponse:
    return service.get(wiki_post_id)


@app.delete(
    "/api/vector-store/wiki-posts/{wiki_post_id}",
    status_code=status.HTTP_204_NO_CONTENT,
)
def delete_wiki_document(
    wiki_post_id: int,
    service: WikiVectorStoreService = Depends(get_vector_store_service),
) -> Response:
    service.delete(wiki_post_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@app.get(
    "/api/vector-store/stats",
    response_model=VectorStoreStatsResponse,
)
def vector_store_stats(
    service: WikiVectorStoreService = Depends(get_vector_store_service),
) -> VectorStoreStatsResponse:
    return service.stats()


@app.post(
    "/api/search/wiki-posts",
    response_model=SemanticSearchResponse,
    response_model_by_alias=True,
)
def search_wiki_documents(
    request: SemanticSearchRequest,
    service: SemanticSearchService = Depends(get_search_service),
) -> SemanticSearchResponse:
    return service.search(request)
