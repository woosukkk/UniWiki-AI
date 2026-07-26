from functools import cached_property
from typing import Protocol, Sequence

import numpy as np


class Embedder(Protocol):
    model_name: str

    def encode(self, texts: Sequence[str]) -> np.ndarray:
        """Return a two-dimensional float array, one row per text."""


class SentenceTransformerEmbedder:
    def __init__(self, model_name: str) -> None:
        self.model_name = model_name

    @cached_property
    def model(self):
        from sentence_transformers import SentenceTransformer

        return SentenceTransformer(self.model_name)

    def encode(self, texts: Sequence[str]) -> np.ndarray:
        if not texts:
            return np.empty((0, 0), dtype=np.float32)
        return self.model.encode(
            list(texts),
            normalize_embeddings=True,
            convert_to_numpy=True,
            show_progress_bar=False,
        ).astype(np.float32)
