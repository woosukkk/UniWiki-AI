from app.chunking import chunk_text


def test_keeps_short_paragraphs_together() -> None:
    chunks = chunk_text("첫 문단입니다.\n\n두 번째 문단입니다.", max_chars=100, overlap_chars=10)

    assert len(chunks) == 1
    assert chunks[0].text == "첫 문단입니다.\n\n두 번째 문단입니다."


def test_splits_long_text_with_overlap() -> None:
    content = "가" * 250

    chunks = chunk_text(content, max_chars=100, overlap_chars=20)

    assert len(chunks) == 3
    assert all(len(chunk.text) <= 100 for chunk in chunks)
    assert chunks[0].text[-20:] == chunks[1].text[:20]


def test_returns_empty_list_for_blank_text() -> None:
    assert chunk_text(" \n\n ", max_chars=100, overlap_chars=10) == []
