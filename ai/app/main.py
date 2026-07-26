from functools import lru_cache

from fastapi import Depends, FastAPI

from app.config import settings
from app.embedder import SentenceTransformerEmbedder
from app.embedding_service import WikiEmbeddingService
from app.models import HealthResponse, WikiDocumentRequest, WikiEmbeddingResponse

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
