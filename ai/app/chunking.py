import re
from dataclasses import dataclass


@dataclass(frozen=True)
class TextChunk:
    index: int
    text: str


def chunk_text(content: str, max_chars: int, overlap_chars: int) -> list[TextChunk]:
    """Split text by paragraphs first, then split oversized paragraphs with overlap."""
    normalized = re.sub(r"\r\n?", "\n", content).strip()
    if not normalized:
        return []

    paragraphs = [part.strip() for part in re.split(r"\n\s*\n", normalized) if part.strip()]
    pieces: list[str] = []

    for paragraph in paragraphs:
        if len(paragraph) <= max_chars:
            pieces.append(paragraph)
            continue
        pieces.extend(_split_long_text(paragraph, max_chars, overlap_chars))

    merged: list[str] = []
    current = ""
    for piece in pieces:
        candidate = f"{current}\n\n{piece}" if current else piece
        if len(candidate) <= max_chars:
            current = candidate
        else:
            if current:
                merged.append(current)
            current = piece
    if current:
        merged.append(current)

    return [TextChunk(index=index, text=text) for index, text in enumerate(merged)]


def _split_long_text(text: str, max_chars: int, overlap_chars: int) -> list[str]:
    chunks: list[str] = []
    start = 0

    while start < len(text):
        hard_end = min(start + max_chars, len(text))
        end = hard_end
        if hard_end < len(text):
            break_at = max(
                text.rfind(". ", start, hard_end),
                text.rfind("다. ", start, hard_end),
                text.rfind(" ", start, hard_end),
            )
            if break_at > start + max_chars // 2:
                end = break_at + 1

        chunks.append(text[start:end].strip())
        if end >= len(text):
            break
        start = max(end - overlap_chars, start + 1)

    return chunks
