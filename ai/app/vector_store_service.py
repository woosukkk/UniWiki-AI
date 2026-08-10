from app.embedding_service import WikiEmbeddingService
from app.models import (
    VectorStoreDocumentResponse,
    VectorStoreStatsResponse,
    VectorStoreWriteResponse,
    WikiDocumentRequest,
)
from app.vector_store import VectorStore


class WikiVectorStoreService:
    def __init__(
        self,
        embedding_service: WikiEmbeddingService,
        vector_store: VectorStore,
    ) -> None:
        self.embedding_service = embedding_service
        self.vector_store = vector_store

    def save(self, document: WikiDocumentRequest) -> VectorStoreWriteResponse:
        embedding = self.embedding_service.embed(document)
        stored_count = self.vector_store.replace_document(embedding)
        stored = self.vector_store.get_document(document.wiki_post_id)
        expected_key = embedding.chunks[0].metadata.source_key if embedding.chunks else ""
        if stored.chunk_count != stored_count or any(
                chunk.metadata.source_key != expected_key
                for chunk in stored.chunks):
            raise RuntimeError("벡터 저장 후 검증에 실패했습니다.")
        return VectorStoreWriteResponse(
            wikiPostId=document.wiki_post_id,
            storedChunkCount=stored_count,
            collection=self.vector_store.collection_name,
        )

    def get(self, wiki_post_id: int) -> VectorStoreDocumentResponse:
        return self.vector_store.get_document(wiki_post_id)

    def delete(self, wiki_post_id: int) -> None:
        self.vector_store.delete_document(wiki_post_id)

    def stats(self) -> VectorStoreStatsResponse:
        return VectorStoreStatsResponse(
            collection=self.vector_store.collection_name,
            count=self.vector_store.count(),
        )
