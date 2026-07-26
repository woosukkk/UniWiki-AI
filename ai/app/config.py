import os
from dataclasses import dataclass
from pathlib import Path


AI_ROOT = Path(__file__).resolve().parents[1]


@dataclass(frozen=True)
class Settings:
    embedding_model: str = os.getenv(
        "EMBEDDING_MODEL",
        "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
    )
    chunk_max_chars: int = int(os.getenv("CHUNK_MAX_CHARS", "800"))
    chunk_overlap_chars: int = int(os.getenv("CHUNK_OVERLAP_CHARS", "120"))
    chroma_persist_dir: str = os.getenv(
        "CHROMA_PERSIST_DIR",
        str(AI_ROOT / "data" / "vectorstore" / "chroma"),
    )
    chroma_collection: str = os.getenv(
        "CHROMA_COLLECTION",
        "uniwiki_wiki_chunks",
    )

    def __post_init__(self) -> None:
        if self.chunk_max_chars < 100:
            raise ValueError("CHUNK_MAX_CHARS는 100 이상이어야 합니다.")
        if not 0 <= self.chunk_overlap_chars < self.chunk_max_chars:
            raise ValueError("CHUNK_OVERLAP_CHARS는 청크 크기보다 작아야 합니다.")


settings = Settings()
