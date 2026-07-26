from pathlib import Path
from typing import Protocol

from app.models import (
    ChunkMetadata,
    VectorStoreDocumentResponse,
    VectorStoreRecord,
    WikiEmbeddingResponse,
)


class VectorStore(Protocol):
    collection_name: str

    def replace_document(self, embedding: WikiEmbeddingResponse) -> int:
        """Insert or replace every chunk for a wiki document."""

    def get_document(self, wiki_post_id: int) -> VectorStoreDocumentResponse:
        """Return every stored chunk for a wiki document."""

    def delete_document(self, wiki_post_id: int) -> None:
        """Delete every stored chunk for a wiki document."""

    def count(self) -> int:
        """Return the total number of chunks."""


class ChromaVectorStore:
    def __init__(self, persist_dir: str, collection_name: str) -> None:
        import chromadb

        Path(persist_dir).mkdir(parents=True, exist_ok=True)
        self.collection_name = collection_name
        self.client = chromadb.PersistentClient(path=persist_dir)
        self.collection = self.client.get_or_create_collection(
            name=collection_name,
            metadata={"description": "UniWiki wiki document chunks"},
        )

    def replace_document(self, embedding: WikiEmbeddingResponse) -> int:
        existing = self.collection.get(
            where={"wikiPostId": embedding.wiki_post_id},
            include=[],
        )
        existing_ids = set(existing["ids"])
        current_ids = {chunk.chunk_id for chunk in embedding.chunks}

        if embedding.chunks:
            self.collection.upsert(
                ids=[chunk.chunk_id for chunk in embedding.chunks],
                embeddings=[chunk.embedding for chunk in embedding.chunks],
                documents=[chunk.text for chunk in embedding.chunks],
                metadatas=[
                    chunk.metadata.model_dump(by_alias=True)
                    for chunk in embedding.chunks
                ],
            )

        stale_ids = list(existing_ids - current_ids)
        if stale_ids:
            self.collection.delete(ids=stale_ids)

        return len(embedding.chunks)

    def get_document(self, wiki_post_id: int) -> VectorStoreDocumentResponse:
        result = self.collection.get(
            where={"wikiPostId": wiki_post_id},
            include=["documents", "metadatas", "embeddings"],
        )
        records = []
        embeddings = result["embeddings"]

        for index, chunk_id in enumerate(result["ids"]):
            metadata = result["metadatas"][index]
            records.append(
                VectorStoreRecord(
                    chunkId=chunk_id,
                    text=result["documents"][index],
                    embedding=embeddings[index].tolist(),
                    metadata=ChunkMetadata.model_validate(metadata),
                )
            )

        records.sort(key=lambda record: record.metadata.chunk_index)
        return VectorStoreDocumentResponse(
            wikiPostId=wiki_post_id,
            chunkCount=len(records),
            chunks=records,
        )

    def delete_document(self, wiki_post_id: int) -> None:
        self.collection.delete(where={"wikiPostId": wiki_post_id})

    def count(self) -> int:
        return self.collection.count()
