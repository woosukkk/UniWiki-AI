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


def test_searches_by_cosine_similarity_and_category(tmp_path: Path) -> None:
    store = create_store(tmp_path)
    first = embedding_response(1, ["수강신청은 학교 포털에서 진행합니다."])
    second = embedding_response(2, ["도서관 운영 시간 안내입니다."])
    first.chunks[0].embedding = [1.0, 0.0, 0.0]
    second.chunks[0].embedding = [0.0, 1.0, 0.0]
    second.chunks[0].metadata.category_id = 3
    store.replace_document(first)
    store.replace_document(second)

    results = store.search([1.0, 0.0, 0.0], top_k=2)
    filtered = store.search([1.0, 0.0, 0.0], top_k=2, category_id=3)

    assert [result.wiki_post_id for result in results] == [1, 2]
    assert results[0].score == 1.0
    assert filtered[0].wiki_post_id == 2
    assert len(filtered) == 1


def test_reads_chunks_only_for_selected_documents(tmp_path: Path) -> None:
    store = create_store(tmp_path)
    store.replace_document(embedding_response(1, ["선택 문서 1", "선택 문서 2"]))
    store.replace_document(embedding_response(2, ["제외 문서"]))

    records = store.records_for_wiki_posts([1])

    assert [record.wiki_post_id for record in records] == [1, 1]
    assert [record.chunk_index for record in records] == [0, 1]


def test_finds_exact_title_without_loading_every_document(tmp_path: Path) -> None:
    store = create_store(tmp_path)
    matching = embedding_response(1, ["멘토링 내용"])
    matching.chunks[0].metadata.title = "우아한테크코스 멘토링"
    store.replace_document(matching)
    store.replace_document(embedding_response(2, ["다른 내용"]))

    records = store.title_records(["우아한테크코스"])

    assert [record.wiki_post_id for record in records] == [1]
