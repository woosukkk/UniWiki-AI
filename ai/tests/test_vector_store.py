from pathlib import Path

from app.models import ChunkMetadata, EmbeddedChunk, WikiEmbeddingResponse
from app.vector_store import ChromaVectorStore


def embedding_response(
    wiki_post_id: int,
    texts: list[str],
) -> WikiEmbeddingResponse:
    chunks = [
        EmbeddedChunk(
            chunkId=f"wiki-{wiki_post_id}-chunk-{index}",
            text=text,
            embedding=[float(index + 1), 0.5, 0.25],
            metadata=ChunkMetadata(
                wikiPostId=wiki_post_id,
                title="테스트 위키",
                categoryId=2,
                chunkIndex=index,
            ),
        )
        for index, text in enumerate(texts)
    ]
    return WikiEmbeddingResponse(
        wikiPostId=wiki_post_id,
        model="test-model",
        dimension=3,
        chunkCount=len(chunks),
        chunks=chunks,
    )


def create_store(path: Path) -> ChromaVectorStore:
    return ChromaVectorStore(str(path), "test_wiki_chunks")


def test_stores_and_reads_document_chunks(tmp_path: Path) -> None:
    store = create_store(tmp_path)

    stored_count = store.replace_document(
        embedding_response(7, ["첫 번째 청크", "두 번째 청크"])
    )
    document = store.get_document(7)

    assert stored_count == 2
    assert store.count() == 2
    assert document.chunk_count == 2
    assert [chunk.text for chunk in document.chunks] == ["첫 번째 청크", "두 번째 청크"]
    assert document.chunks[0].metadata.wiki_post_id == 7
    assert len(document.chunks[0].embedding) == 3


def test_replaces_changed_document_and_removes_stale_chunks(tmp_path: Path) -> None:
    store = create_store(tmp_path)
    store.replace_document(embedding_response(7, ["기존 1", "기존 2", "기존 3"]))

    store.replace_document(embedding_response(7, ["수정된 내용"]))
    document = store.get_document(7)

    assert store.count() == 1
    assert document.chunk_count == 1
    assert document.chunks[0].text == "수정된 내용"


def test_persists_and_deletes_document_chunks(tmp_path: Path) -> None:
    store = create_store(tmp_path)
    store.replace_document(embedding_response(9, ["영속 저장 청크"]))

    reopened_store = create_store(tmp_path)
    assert reopened_store.get_document(9).chunk_count == 1

    reopened_store.delete_document(9)
    assert reopened_store.get_document(9).chunk_count == 0
    assert reopened_store.count() == 0
