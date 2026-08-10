from pathlib import Path
from typing import Protocol

from app.models import (
    ChunkMetadata,
    SemanticSearchResult,
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

    def search(
        self,
        query_embedding: list[float],
        top_k: int,
        category_id: int | None = None,
    ) -> list[SemanticSearchResult]:
        """Return chunks ordered by cosine similarity."""

    def records_for_wiki_posts(
        self,
        wiki_post_ids: list[int],
    ) -> list[SemanticSearchResult]:
        """Return chunks only for the requested wiki documents."""

    def records_for_categories(
        self,
        category_ids: list[int],
    ) -> list[SemanticSearchResult]:
        """Return every chunk in the selected categories."""

    def keyword_records(
        self,
        terms: list[str],
        category_id: int | None = None,
        limit: int = 20,
    ) -> list[SemanticSearchResult]:
        """Return a bounded set of chunks whose content contains a query term."""


class ChromaVectorStore:
    def __init__(self, persist_dir: str, collection_name: str) -> None:
        import chromadb

        Path(persist_dir).mkdir(parents=True, exist_ok=True)
        self.collection_name = collection_name
        self.client = chromadb.PersistentClient(path=persist_dir)
        self.collection = self.client.get_or_create_collection(
            name=collection_name,
            metadata={"description": "UniWiki wiki document chunks"},
            configuration={"hnsw": {"space": "cosine"}},
        )
        current_space = self.collection.configuration["hnsw"]["space"]
        if current_space != "cosine":
            raise RuntimeError(
                f"Chroma 컬렉션 거리 함수가 cosine이 아닙니다: {current_space}"
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

    def search(
        self,
        query_embedding: list[float],
        top_k: int,
        category_id: int | None = None,
    ) -> list[SemanticSearchResult]:
        collection_count = self.collection.count()
        if collection_count == 0:
            return []

        where = {"categoryId": category_id} if category_id is not None else None
        result = self.collection.query(
            query_embeddings=[query_embedding],
            n_results=min(top_k, collection_count),
            where=where,
            include=["documents", "metadatas", "distances"],
        )

        records = []
        for index, chunk_id in enumerate(result["ids"][0]):
            metadata = result["metadatas"][0][index]
            distance = float(result["distances"][0][index])
            records.append(
                SemanticSearchResult(
                    chunkId=chunk_id,
                    wikiPostId=metadata["wikiPostId"],
                    title=metadata["title"],
                    content=result["documents"][0][index],
                    categoryId=metadata["categoryId"],
                    chunkIndex=metadata["chunkIndex"],
                    documentType=metadata.get("documentType", "GENERAL"),
                    score=max(-1.0, min(1.0, 1.0 - distance)),
                )
            )
        return records

    def records_for_wiki_posts(
        self,
        wiki_post_ids: list[int],
    ) -> list[SemanticSearchResult]:
        unique_ids = list(dict.fromkeys(wiki_post_ids))
        if not unique_ids:
            return []
        result = self.collection.get(
            where={"wikiPostId": {"$in": unique_ids}},
            include=["documents", "metadatas"],
        )
        return [
            SemanticSearchResult(
                chunkId=chunk_id,
                wikiPostId=metadata["wikiPostId"],
                title=metadata["title"],
                content=result["documents"][index],
                categoryId=metadata["categoryId"],
                chunkIndex=metadata["chunkIndex"],
                documentType=metadata.get("documentType", "GENERAL"),
                score=0.0,
            )
            for index, (chunk_id, metadata) in enumerate(
                zip(result["ids"], result["metadatas"])
            )
        ]

    def records_for_categories(self, category_ids: list[int]) -> list[SemanticSearchResult]:
        unique_ids = list(dict.fromkeys(category_ids))
        if not unique_ids:
            return []
        result = self.collection.get(
            where={"categoryId": {"$in": unique_ids}},
            include=["documents", "metadatas"],
        )
        return [
            SemanticSearchResult(
                chunkId=chunk_id,
                wikiPostId=metadata["wikiPostId"],
                title=metadata["title"],
                content=result["documents"][index],
                categoryId=metadata["categoryId"],
                chunkIndex=metadata["chunkIndex"],
                documentType=metadata.get("documentType", "GENERAL"),
                score=0.0,
            )
            for index, (chunk_id, metadata) in enumerate(
                zip(result["ids"], result["metadatas"])
            )
        ]

    def keyword_records(
        self,
        terms: list[str],
        category_id: int | None = None,
        limit: int = 40,
    ) -> list[SemanticSearchResult]:
        unique_terms = list(dict.fromkeys(term for term in terms if len(term) >= 2))
        if not unique_terms:
            return []

        where = {"categoryId": category_id} if category_id is not None else None
        records = []
        seen_chunk_ids = set()
        seen_wiki_post_ids = set()
        per_term_limit = max(2, min(5, limit // len(unique_terms)))
        for term in unique_terms:
            title_where = {"title": term}
            if category_id is not None:
                title_where = {"$and": [{"categoryId": category_id}, title_where]}
            results = [
                self.collection.get(
                    where=title_where,
                    limit=per_term_limit,
                    include=["documents", "metadatas"],
                ),
                self.collection.get(
                    where=where,
                    where_document={"$contains": term},
                    limit=per_term_limit,
                    include=["documents", "metadatas"],
                ),
            ]
            for result in results:
                for index, (chunk_id, metadata) in enumerate(
                        zip(result["ids"], result["metadatas"])):
                    if chunk_id in seen_chunk_ids:
                        continue
                    if metadata["wikiPostId"] in seen_wiki_post_ids:
                        continue
                    seen_chunk_ids.add(chunk_id)
                    seen_wiki_post_ids.add(metadata["wikiPostId"])
                    records.append(SemanticSearchResult(
                        chunkId=chunk_id,
                        wikiPostId=metadata["wikiPostId"],
                        title=metadata["title"],
                        content=result["documents"][index],
                        categoryId=metadata["categoryId"],
                        chunkIndex=metadata["chunkIndex"],
                        documentType=metadata.get("documentType", "GENERAL"),
                        score=0.0,
                    ))
                    if len(records) >= limit:
                        return records
        return records
