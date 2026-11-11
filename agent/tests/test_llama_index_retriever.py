from types import SimpleNamespace

from llama_index.core.schema import TextNode

from app.services.knowledge_base.llama_index_retriever import LlamaIndexKnowledgeRetriever
from app.services.log_diagnosis.llama_index_code_retriever import LlamaIndexCodeKnowledgeRetriever


class FakeEmbeddingModel:
    def __init__(self) -> None:
        self.queries: list[str] = []

    def get_query_embedding(self, query: str) -> list[float]:
        self.queries.append(query)
        return [1.0, 0.0, 0.0]


class FakeMilvusVectorStore:
    def __init__(self, nodes: list[TextNode]) -> None:
        self.nodes = nodes
        self.calls = []

    def query(self, query, **kwargs):
        self.calls.append((query, kwargs))
        return SimpleNamespace(
            nodes=self.nodes,
            similarities=[0.82 for _ in self.nodes],
            ids=[node.node_id for node in self.nodes],
        )


def test_retriever_queries_global_and_current_course_documents() -> None:
    node = TextNode(
        id_="java_hashmap_001_chunk_0001",
        text="HashMap 是 Java 中常用的 Map 实现类。",
        metadata={
            "document_id": "java_hashmap_001",
            "chunk_id": "chunk_0001",
            "course_id": 3,
            "study_mode": "OFFLINE",
            "title": "Java HashMap 详解",
        },
    )
    embedding_model = FakeEmbeddingModel()
    vector_store = FakeMilvusVectorStore([node])
    retriever = LlamaIndexKnowledgeRetriever(
        course_id=3,
        study_mode="OFFLINE",
        top_k=5,
        vector_store=vector_store,
        embedding_model=embedding_model,
    )

    nodes = retriever.retrieve("HashMap 是什么")

    assert embedding_model.queries == ["HashMap 是什么"]
    assert len(nodes) == 1
    assert nodes[0].score == 0.82
    assert nodes[0].node.get_content() == "HashMap 是 Java 中常用的 Map 实现类。"
    assert nodes[0].node.metadata["document_id"] == "java_hashmap_001"

    query, kwargs = vector_store.calls[0]
    assert query.query_embedding == [1.0, 0.0, 0.0]
    assert query.similarity_top_k == 5
    assert kwargs["string_expr"] == 'course_id in [0, 3] and study_mode in ["ALL", "OFFLINE"]'
    assert kwargs["milvus_search_config"]["metric_type"] == "COSINE"


def test_retriever_without_course_only_queries_global_documents() -> None:
    vector_store = FakeMilvusVectorStore([])
    retriever = LlamaIndexKnowledgeRetriever(
        study_mode="ONLINE",
        top_k=5,
        vector_store=vector_store,
        embedding_model=FakeEmbeddingModel(),
    )

    nodes = retriever.retrieve("缴费说明")

    assert nodes == []
    assert vector_store.calls[0][1]["string_expr"] == 'course_id == 0 and study_mode in ["ALL", "ONLINE"]'


def test_code_retriever_queries_log_diagnosis_collection_filter() -> None:
    node = TextNode(
        id_="kd_java_auth_v1_chunk_0001",
        text="AuthServiceImpl.login 登录失败会抛出用户名或密码错误。",
        metadata={
            "document_id": "kd_java_auth",
            "chunk_id": "chunk_0001",
            "knowledge_domain": "LOG_DIAGNOSIS",
            "permission_scope": "log_diagnosis",
            "method_heading": "AuthServiceImpl.login(LoginReq request)",
        },
    )
    embedding_model = FakeEmbeddingModel()
    vector_store = FakeMilvusVectorStore([node])
    retriever = LlamaIndexCodeKnowledgeRetriever(
        top_k=7,
        vector_store=vector_store,
        embedding_model=embedding_model,
    )

    nodes = retriever.retrieve("用户名或密码错误")

    assert embedding_model.queries == ["用户名或密码错误"]
    assert len(nodes) == 1
    assert nodes[0].node.get_content() == "AuthServiceImpl.login 登录失败会抛出用户名或密码错误。"
    query, kwargs = vector_store.calls[0]
    assert query.similarity_top_k == 7
    assert kwargs["string_expr"] == 'knowledge_domain == "LOG_DIAGNOSIS" and permission_scope == "log_diagnosis"'
    assert kwargs["milvus_search_config"]["metric_type"] == "COSINE"
