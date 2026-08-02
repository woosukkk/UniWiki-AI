from functools import cached_property
from typing import Protocol, Sequence

import numpy as np


class Embedder(Protocol):
    model_name: str

    def encode(self, texts: Sequence[str]) -> np.ndarray:
        """Return a two-dimensional float array, one row per text."""


class FastEmbedEmbedder:
    def __init__(self, model_name: str) -> None:
        self.model_name = model_name

    @cached_property
    def model(self):
        from fastembed import TextEmbedding

        return TextEmbedding(model_name=self.model_name, threads=1)

    def encode(self, texts: Sequence[str]) -> np.ndarray:
        if not texts:
            return np.empty((0, 0), dtype=np.float32)
        vectors = list(self.model.embed(list(texts), batch_size=8))
        return np.asarray(vectors, dtype=np.float32)
