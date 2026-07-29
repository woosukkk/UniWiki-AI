from app.chunking import chunk_text
from app.embedder import Embedder
from app.models import (
    ChunkMetadata,
    EmbeddedChunk,
    WikiDocumentRequest,
    WikiEmbeddingResponse,
)


class WikiEmbeddingService:
    def __init__(self, embedder: Embedder, max_chars: int, overlap_chars: int) -> None:
        self.embedder = embedder
        self.max_chars = max_chars
        self.overlap_chars = overlap_chars

    def embed(self, document: WikiDocumentRequest) -> WikiEmbeddingResponse:
        text_chunks = chunk_text(document.content, self.max_chars, self.overlap_chars)
        embedding_inputs = [
            f"제목: {document.title}\n본문: {chunk.text}"
            for chunk in text_chunks
        ]
        vectors = self.embedder.encode(embedding_inputs)

        if len(vectors) != len(text_chunks):
            raise RuntimeError("청크 수와 임베딩 수가 일치하지 않습니다.")

        dimension = int(vectors.shape[1]) if len(vectors) else 0
        chunks = [
            EmbeddedChunk(
                chunkId=f"wiki-{document.wiki_post_id}-chunk-{chunk.index}",
                text=chunk.text,
                embedding=vector.tolist(),
                metadata=ChunkMetadata(
                    wikiPostId=document.wiki_post_id,
                    title=document.title,
                    categoryId=document.category_id,
                    chunkIndex=chunk.index,
                ),
            )
            for chunk, vector in zip(text_chunks, vectors)
        ]

        return WikiEmbeddingResponse(
            wikiPostId=document.wiki_post_id,
            model=self.embedder.model_name,
            dimension=dimension,
            chunkCount=len(chunks),
            chunks=chunks,
        )
