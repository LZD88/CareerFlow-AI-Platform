from pydantic import BaseModel, Field


class TextEmbeddingRequest(BaseModel):
    text: str = Field(default="", description="需要向量化的文本")


class TextEmbeddingResponse(BaseModel):
    model: str
    dimension: int
    embedding: list[float]


class EmbeddingHealthResponse(BaseModel):
    ok: bool
    model: str
    base_url: str
    path: str
    dimension: int
    message: str
