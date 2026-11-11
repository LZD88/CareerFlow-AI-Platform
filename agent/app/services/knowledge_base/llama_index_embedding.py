from llama_index.core.embeddings import BaseEmbedding
from pydantic import PrivateAttr

from app.config.settings import get_settings
from app.services.knowledge_base.embedding_service import EmbeddingService


class LlamaIndexVolcengineEmbedding(BaseEmbedding):
    """
    火山方舟向量化模型的 LlamaIndex Embedding 适配器。

    LlamaIndex 在检索时需要一个 `BaseEmbedding` 实现，用来把用户 query 转成向量。
    你项目里已经有 `EmbeddingService`，它负责调用火山向量化 API 并校验维度。

    这个类的作用就是把已有的 `EmbeddingService.embed_text()` 包装成 LlamaIndex 能识别的接口：
    - get_query_embedding(query)：检索问题向量化；
    - get_text_embedding(text)：文本向量化；
    - async 版本：为了兼容 LlamaIndex 异步接口。
    """

    _embedding_service: EmbeddingService = PrivateAttr()

    def __init__(self, embedding_service: EmbeddingService | None = None) -> None:
        settings = get_settings()
        super().__init__(model_name=settings.embedding_model)
        self._embedding_service = embedding_service or EmbeddingService()

    def _get_query_embedding(self, query: str) -> list[float]:
        return self._embedding_service.embed_text(query)

    def _get_text_embedding(self, text: str) -> list[float]:
        return self._embedding_service.embed_text(text)

    def _get_text_embeddings(self, texts: list[str]) -> list[list[float]]:
        return [self._embedding_service.embed_text(text) for text in texts]

    async def _aget_query_embedding(self, query: str) -> list[float]:
        return self._get_query_embedding(query)

    async def _aget_text_embedding(self, text: str) -> list[float]:
        return self._get_text_embedding(text)
