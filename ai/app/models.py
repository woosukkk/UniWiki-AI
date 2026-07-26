from pydantic import BaseModel, ConfigDict, Field, field_validator


class WikiDocumentRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    wiki_post_id: int = Field(alias="wikiPostId", gt=0)
    title: str = Field(min_length=1, max_length=200)
    content: str = Field(min_length=1)
    category_id: int = Field(alias="categoryId", gt=0)

    @field_validator("title", "content")
    @classmethod
    def reject_blank_text(cls, value: str) -> str:
        value = value.strip()
        if not value:
            raise ValueError("공백만 입력할 수 없습니다.")
        return value


class ChunkMetadata(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    wiki_post_id: int = Field(alias="wikiPostId")
    title: str
    category_id: int = Field(alias="categoryId")
    chunk_index: int = Field(alias="chunkIndex")


class EmbeddedChunk(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    chunk_id: str = Field(alias="chunkId")
    text: str
    embedding: list[float]
    metadata: ChunkMetadata


class WikiEmbeddingResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    wiki_post_id: int = Field(alias="wikiPostId")
    model: str
    dimension: int
    chunk_count: int = Field(alias="chunkCount")
    chunks: list[EmbeddedChunk]


class HealthResponse(BaseModel):
    status: str
    model: str
