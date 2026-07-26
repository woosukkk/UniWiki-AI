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


class VectorStoreWriteResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    wiki_post_id: int = Field(alias="wikiPostId")
    stored_chunk_count: int = Field(alias="storedChunkCount")
    collection: str


class VectorStoreRecord(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    chunk_id: str = Field(alias="chunkId")
    text: str
    embedding: list[float]
    metadata: ChunkMetadata


class VectorStoreDocumentResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    wiki_post_id: int = Field(alias="wikiPostId")
    chunk_count: int = Field(alias="chunkCount")
    chunks: list[VectorStoreRecord]


class VectorStoreStatsResponse(BaseModel):
    collection: str
    count: int


class SemanticSearchRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    query: str = Field(min_length=1, max_length=1000)
    top_k: int | None = Field(default=None, alias="topK", ge=1, le=50)
    category_id: int | None = Field(default=None, alias="categoryId", gt=0)

    @field_validator("query")
    @classmethod
    def reject_blank_query(cls, value: str) -> str:
        value = value.strip()
        if not value:
            raise ValueError("검색 질문은 공백일 수 없습니다.")
        return value


class SemanticSearchResult(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    chunk_id: str = Field(alias="chunkId")
    wiki_post_id: int = Field(alias="wikiPostId")
    title: str
    content: str
    category_id: int = Field(alias="categoryId")
    chunk_index: int = Field(alias="chunkIndex")
    score: float


class SemanticSearchResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    query: str
    top_k: int = Field(alias="topK")
    result_count: int = Field(alias="resultCount")
    results: list[SemanticSearchResult]


class RagAnswerRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    question: str = Field(min_length=1, max_length=1000)
    category_id: int | None = Field(default=None, alias="categoryId", gt=0)

    @field_validator("question")
    @classmethod
    def reject_blank_question(cls, value: str) -> str:
        value = value.strip()
        if not value:
            raise ValueError("Question must not be blank.")
        return value


class RagAnswerResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    question: str
    answer: str
    grounded: bool
    retrieved_chunk_count: int = Field(alias="retrievedChunkCount")
