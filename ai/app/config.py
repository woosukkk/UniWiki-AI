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
    search_top_k: int = int(os.getenv("SEARCH_TOP_K", "5"))
    rag_top_k: int = int(os.getenv("RAG_TOP_K", "5"))
    rag_min_score: float = float(os.getenv("RAG_MIN_SCORE", "0.35"))
    rag_max_context_chars: int = int(os.getenv("RAG_MAX_CONTEXT_CHARS", "12000"))
    openai_api_key: str | None = os.getenv("OPENAI_API_KEY")
    openai_model: str = os.getenv("OPENAI_MODEL", "gpt-5.6-sol")
    openai_timeout_seconds: float = float(os.getenv("OPENAI_TIMEOUT_SECONDS", "30"))
    openai_max_output_tokens: int = int(os.getenv("OPENAI_MAX_OUTPUT_TOKENS", "1000"))
    summary_max_chars: int = int(os.getenv("SUMMARY_MAX_CHARS", "600"))
    summary_context_chars: int = int(os.getenv("SUMMARY_CONTEXT_CHARS", "12000"))

    def __post_init__(self) -> None:
        if self.chunk_max_chars < 100:
            raise ValueError("CHUNK_MAX_CHARS는 100 이상이어야 합니다.")
        if not 0 <= self.chunk_overlap_chars < self.chunk_max_chars:
            raise ValueError("CHUNK_OVERLAP_CHARS는 청크 크기보다 작아야 합니다.")
        if not 1 <= self.search_top_k <= 50:
            raise ValueError("SEARCH_TOP_K는 1에서 50 사이여야 합니다.")
        if not 1 <= self.rag_top_k <= 20:
            raise ValueError("RAG_TOP_K must be between 1 and 20.")
        if not -1.0 <= self.rag_min_score <= 1.0:
            raise ValueError("RAG_MIN_SCORE must be between -1 and 1.")
        if self.rag_max_context_chars < 100:
            raise ValueError("RAG_MAX_CONTEXT_CHARS must be at least 100.")
        if self.openai_timeout_seconds <= 0:
            raise ValueError("OPENAI_TIMEOUT_SECONDS must be positive.")
        if self.openai_max_output_tokens < 1:
            raise ValueError("OPENAI_MAX_OUTPUT_TOKENS must be positive.")
        if not 100 <= self.summary_max_chars <= 2000:
            raise ValueError("SUMMARY_MAX_CHARS must be between 100 and 2000.")
        if self.summary_context_chars < 4000:
            raise ValueError("SUMMARY_CONTEXT_CHARS must be at least 4000.")


settings = Settings()
