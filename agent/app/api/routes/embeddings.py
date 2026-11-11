from fastapi import APIRouter, HTTPException

from app.models.embedding import EmbeddingHealthResponse, TextEmbeddingRequest, TextEmbeddingResponse
from app.services.knowledge_base.embedding_service import EmbeddingService

router = APIRouter()
service = EmbeddingService()


@router.get("/embeddings/health", response_model=EmbeddingHealthResponse)
def embedding_health() -> dict:
    """
    真实调用一次 Embedding 模型，验证火山方舟向量化配置是否可用。
    """
    try:
        return service.health()
    except ValueError as ex:
        raise HTTPException(status_code=400, detail=str(ex)) from ex


@router.post("/embeddings/text", response_model=TextEmbeddingResponse)
def create_text_embedding(request: TextEmbeddingRequest) -> dict:
    """
    将文本转换成向量。

    开发调试用：可以确认模型返回维度是否和 Milvus collection 一致。
    """
    try:
        embedding = service.embed_text(request.text)
    except ValueError as ex:
        raise HTTPException(status_code=400, detail=str(ex)) from ex
    return {
        "model": service.settings.embedding_model,
        "dimension": len(embedding),
        "embedding": embedding,
    }
