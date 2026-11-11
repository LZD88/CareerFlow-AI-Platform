import logging

from llama_index.core.retrievers import BaseRetriever
from llama_index.core.schema import NodeWithScore, QueryBundle
from llama_index.core.vector_stores.types import VectorStoreQuery
from llama_index.vector_stores.milvus import MilvusVectorStore

from app.config.settings import get_settings
from app.services.knowledge_base.llama_index_embedding import LlamaIndexVolcengineEmbedding

logger = logging.getLogger(__name__)


class LlamaIndexCodeKnowledgeRetriever(BaseRetriever):
    """
    日志诊断代码知识库的 LlamaIndex Retriever。

    它和学生端 LlamaIndexKnowledgeRetriever 的写法一致：由 LlamaIndex 负责
    embedding query、MilvusVectorStore 查询和 NodeWithScore 结果封装。
    区别是这里固定查询代码知识库 collection，并过滤 LOG_DIAGNOSIS 数据。
    """

    def __init__(
        self,
        *,
        top_k: int = 5,
        vector_store: MilvusVectorStore | None = None,
        embedding_model: LlamaIndexVolcengineEmbedding | None = None,
    ) -> None:
        super().__init__()
        self.settings = get_settings()
        self.top_k = top_k
        self.embedding_model = embedding_model or LlamaIndexVolcengineEmbedding()
        self.vector_store = vector_store or self._build_vector_store()

    def _retrieve(self, query_bundle: QueryBundle) -> list[NodeWithScore]:
        query = query_bundle.query_str.strip()
        if not query:
            return []

        query_embedding = self.embedding_model.get_query_embedding(query)
        result = self.vector_store.query(
            VectorStoreQuery(
                query_embedding=query_embedding,
                similarity_top_k=self.top_k,
                embedding_field="vector",
                output_fields=self._output_fields(),
            ),
            string_expr=self._build_filter(),
            milvus_search_config={"metric_type": "COSINE", "params": {}},
        )

        nodes: list[NodeWithScore] = []
        for index, node in enumerate(result.nodes or []):
            score = (result.similarities or [None])[index]
            node_id = (result.ids or [None])[index]
            if node_id:
                node.id_ = str(node_id)
            nodes.append(NodeWithScore(node=node, score=float(score or 0)))
        return nodes

    def _build_vector_store(self) -> MilvusVectorStore:
        return MilvusVectorStore(
            uri=self.settings.milvus_uri,
            token=self.settings.milvus_token or "",
            db_name=self.settings.milvus_db_name,
            collection_name=self.settings.milvus_code_collection,
            dim=self.settings.milvus_vector_dim,
            embedding_field="vector",
            text_key="content",
            similarity_metric="COSINE",
            output_fields=self._output_fields(),
            use_async_client=False,
        )

    def _build_filter(self) -> str:
        return 'knowledge_domain == "LOG_DIAGNOSIS" and permission_scope == "log_diagnosis"'

    def _output_fields(self) -> list[str]:
        return [
            "document_id",
            "chunk_id",
            "course_id",
            "class_id",
            "study_mode",
            "permission_scope",
            "title",
            "header_path",
            "content",
            "version",
            "source_type",
            "source_path",
            "knowledge_domain",
            "chunk_type",
            "method_name",
            "method_heading",
        ]
