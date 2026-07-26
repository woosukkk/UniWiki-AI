import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    embedding_model: str = os.getenv(
        "EMBEDDING_MODEL",
        "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
    )
    chunk_max_chars: int = int(os.getenv("CHUNK_MAX_CHARS", "800"))
    chunk_overlap_chars: int = int(os.getenv("CHUNK_OVERLAP_CHARS", "120"))

    def __post_init__(self) -> None:
        if self.chunk_max_chars < 100:
            raise ValueError("CHUNK_MAX_CHARS는 100 이상이어야 합니다.")
        if not 0 <= self.chunk_overlap_chars < self.chunk_max_chars:
            raise ValueError("CHUNK_OVERLAP_CHARS는 청크 크기보다 작아야 합니다.")


settings = Settings()
