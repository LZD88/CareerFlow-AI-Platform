import logging

from llama_index.core.retrievers import BaseRetriever
from llama_index.core.schema import NodeWithScore, QueryBundle
from llama_index.core.vector_stores.types import VectorStoreQuery
from llama_index.vector_stores.milvus import MilvusVectorStore

from app.config.settings import get_settings
from app.services.knowledge_base.llama_index_embedding import LlamaIndexVolcengineEmbedding

logger = logging.getLogger(__name__)


class LlamaIndexKnowledgeRetriever(BaseRetriever):
    """
    使用 LlamaIndex 原生 MilvusVectorStore 查询知识库的 Retriever。

    Retriever 在 RAG 里的职责很单一：只负责“查资料”，不负责“组织最终回答”。
    也就是说，它接收一个 query，返回一批带相关性分数的知识节点。

    当前版本已经不再调用项目旧的 `KnowledgeSearchService.search()`。
    查询链路变成：
    1. LlamaIndexVolcengineEmbedding：把 query 转成火山 embedding 向量；
    2. MilvusVectorStore.query：由 LlamaIndex 的 Milvus 适配器执行向量检索；
    3. NodeWithScore：返回 LlamaIndex 标准知识节点。

    这样学生问答层拿到的就是完整的 LlamaIndex 检索结果。
    后续要接 reranker、query engine、chat engine 时，可以继续沿用这个 retriever。
    """

    def __init__(
        self,
        *,
        course_id: int | None = None,
        study_mode: str | None = None,
        top_k: int = 5,
        vector_store: MilvusVectorStore | None = None,
        embedding_model: LlamaIndexVolcengineEmbedding | None = None,
    ) -> None:
        # BaseRetriever 是 LlamaIndex 的检索基类。
        # 外部调用 retriever.retrieve("问题") 时，LlamaIndex 会做一些通用处理，
        # 最终回调到本类实现的 `_retrieve(QueryBundle)` 方法。
        super().__init__()

        self.settings = get_settings()

        # course_id/study_mode 是 Java 传来的学生上下文。
        # course_id 用于优先匹配当前课程资料；study_mode 用于区分线上/线下/全部学员资料。
        self.course_id = course_id
        self.study_mode = (study_mode or "").upper()

        # top_k 表示最多召回多少个 chunk。
        # 召回太少可能资料不足，召回太多会增加 prompt token 和噪声。
        self.top_k = top_k

        # 支持测试时注入 fake vector_store / embedding_model。
        # 生产环境不传时，就使用真实火山 embedding 和真实 MilvusVectorStore。
        self.embedding_model = embedding_model or LlamaIndexVolcengineEmbedding()
        self.vector_store = vector_store or self._build_vector_store()

    def _retrieve(self, query_bundle: QueryBundle) -> list[NodeWithScore]:
        """
        LlamaIndex 真正执行检索时会调用这个方法。

        参数不是直接传字符串，而是 `QueryBundle`，这是 LlamaIndex 的标准查询对象。
        后续如果接 query rewrite、多路检索、embedding query 等能力，QueryBundle 可以承载更多信息。

        返回值必须是 `list[NodeWithScore]`：
        - Node 表示一段可被大模型引用的知识文本；
        - Score 表示这段文本和问题的相关性。
        """
        query = query_bundle.query_str.strip()
        if not query:
            return []

        # LlamaIndex 的 VectorStoreQuery 需要 query_embedding。
        # 这里通过 LlamaIndexVolcengineEmbedding 调火山向量化 API，保证查询向量和入库向量维度一致。
        query_embedding = self.embedding_model.get_query_embedding(query)

        # 按当前课程 + 学习方式检索。
        # 没绑定课程的全局资料会写成 course_id=0，因此同一轮检索会同时覆盖全局资料和课程资料。
        nodes = self._query_vector_store(
            query_embedding=query_embedding,
            course_id=self.course_id,
            study_mode=self.study_mode,
            top_k=self.top_k,
        )
        return nodes

    def _query_vector_store(
        self,
        *,
        query_embedding: list[float],
        course_id: int | None,
        study_mode: str | None,
        top_k: int,
    ) -> list[NodeWithScore]:
        """
        通过 LlamaIndex MilvusVectorStore 执行向量查询。

        注意这里已经不是直接调用 pymilvus，也不是调用旧的 KnowledgeSearchService。
        真正访问 Milvus 的动作发生在 `self.vector_store.query(...)` 里面。
        """
        result = self.vector_store.query(
            VectorStoreQuery(
                query_embedding=query_embedding,
                similarity_top_k=top_k,
                embedding_field="vector",
                output_fields=self._output_fields(),
            ),
            string_expr=self._build_filter(course_id=course_id, study_mode=study_mode),
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
        """
        构造 LlamaIndex 的 MilvusVectorStore。

        我们复用当前已经建好的 collection：
        - 向量字段：vector；
        - 文本字段：content；
        - 主键字段：id；
        - 动态字段：document_id / chunk_id / course_id / study_mode / title ...

        这里通过 embedding_field/text_key/output_fields 把当前 schema 告诉 LlamaIndex。
        """
        return MilvusVectorStore(
            uri=self.settings.milvus_uri,
            token=self.settings.milvus_token or "",
            db_name=self.settings.milvus_db_name,
            collection_name=self.settings.milvus_collection,
            dim=self.settings.milvus_vector_dim,
            embedding_field="vector",
            text_key="content",
            similarity_metric="COSINE",
            output_fields=self._output_fields(),
            use_async_client=False,
        )

    def _build_filter(self, *, course_id: int | None, study_mode: str | None) -> str:
        """
        构造 Milvus 标量过滤表达式。

        这里仍然使用 Milvus 的 string_expr 语法，但调用入口已经交给 LlamaIndexVectorStore。
        后续如果要完全用 LlamaIndex MetadataFilters，也可以在这里继续替换。
        """
        filters: list[str] = []
        if course_id is not None:
            filters.append(f"course_id in [0, {course_id}]")
        else:
            filters.append("course_id == 0")
        if study_mode:
            filters.append(f'study_mode in ["ALL", "{self._escape_string(study_mode)}"]')
        else:
            filters.append('study_mode == "ALL"')
        return " and ".join(filters)

    def _escape_string(self, value: str) -> str:
        return value.replace("\\", "\\\\").replace('"', '\\"')

    def _output_fields(self) -> list[str]:
        """
        告诉 LlamaIndex 从 Milvus hit 里取哪些字段。

        `content` 会作为 TextNode.text；
        其他字段会进入 TextNode.metadata，供 prompt、引用来源、检索日志使用。
        """
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
        ]
