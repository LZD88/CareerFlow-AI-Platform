from llama_index.core.schema import NodeWithScore, TextNode

from app.models.diagnose import DiagnoseLogRequest
from app.services.log_diagnosis.code_knowledge_search_service import CodeKnowledgeSearchService
from app.services.log_diagnosis.rag_service import RagService
from app.services.log_diagnosis.volcengine_knowledge_search_service import VolcengineKnowledgeSearchService


class FakeCodeRetriever:
    def __init__(self) -> None:
        self.queries: list[str] = []

    def retrieve(self, query: str) -> list[NodeWithScore]:
        self.queries.append(query)
        return [
            NodeWithScore(
                node=TextNode(
                    text="## AuthServiceImpl.login(LoginReq request)\n异常: BusinessException(401, \"用户名或密码错误\")",
                    metadata={
                        "source_path": "YQ-AI-ADMIN/src/main/java/cn/yq/modules/auth/service/impl/AuthServiceImpl.java",
                        "header_path": "AuthServiceImpl > AuthServiceImpl.login(LoginReq request)",
                        "method_heading": "AuthServiceImpl.login(LoginReq request)",
                    },
                ),
                score=0.87,
            )
        ]


class FakeRetrieverFactory:
    def __init__(self) -> None:
        self.top_k = 0
        self.retriever = FakeCodeRetriever()

    def __call__(self, top_k: int) -> FakeCodeRetriever:
        self.top_k = top_k
        return self.retriever


def test_code_knowledge_search_uses_llama_index_retriever() -> None:
    fake_factory = FakeRetrieverFactory()
    service = CodeKnowledgeSearchService(retriever_factory=fake_factory)

    contexts = service.retrieve(
        DiagnoseLogRequest(
            guid="g-1",
            requestUri="/api/auth/login",
            httpMethod="POST",
            controller="AuthController.login",
            requestArgs='[{"name":"request","value":{"username":"admin","password":"****"}}]',
            exceptionClass="BusinessException",
            exceptionMessage="用户名或密码错误",
            topStackFrame="cn.yq.modules.auth.service.impl.AuthServiceImpl.login(AuthServiceImpl.java:45)",
            rawLogs=["BusinessException: 用户名或密码错误"],
        ),
        top_k=5,
    )

    query = fake_factory.retriever.queries[0]
    assert fake_factory.top_k == 5
    assert "堆栈类: cn.yq.modules.auth.service.impl.AuthServiceImpl" in query
    assert "堆栈方法: login" in query
    assert '"username":"admin"' in query
    assert "AuthServiceImpl.login(LoginReq request)" in contexts[0]
    assert "相似度: 0.8700" in contexts[0]
    assert "用户名或密码错误" in contexts[0]


def test_rag_service_delegates_to_code_knowledge_search_service() -> None:
    class FakeCodeKnowledgeSearchService:
        def retrieve(self, request: DiagnoseLogRequest) -> list[str]:
            return [f"code-context:{request.guid}"]

    service = RagService()
    service.code_knowledge_search_service = FakeCodeKnowledgeSearchService()

    contexts = service.retrieve(DiagnoseLogRequest(guid="g-2"))

    assert contexts == ["code-context:g-2"]


def test_rag_service_can_delegate_to_volcengine_knowledge_search_service() -> None:
    class FakeVolcengineKnowledgeSearchService:
        def retrieve(self, request: DiagnoseLogRequest) -> list[str]:
            return [f"volcengine-context:{request.guid}"]

    service = RagService()
    service.volcengine_knowledge_search_service = FakeVolcengineKnowledgeSearchService()

    contexts = service.retrieve_from_volcengine(DiagnoseLogRequest(guid="g-3"))

    assert contexts == ["volcengine-context:g-3"]


def test_volcengine_extract_contexts_reads_retrieve_result_list() -> None:
    response = {
        "code": 0,
        "message": "success",
        "data": {
            "count": 1,
            "result_list": [
                {
                    "content": "问题:注册抖音的方式有哪些\n答案:有手机号、邮箱等方式",
                    "chunk_type": "structured",
                    "score": 0.6423748731613159,
                    "rerank_score": 0.929823701560032,
                    "rerank_position": 1,
                    "doc_info": {
                        "doc_name": "未命名表格.xlsx",
                    },
                    "table_chunk_fields": [
                        {"field_name": "问题", "field_value": "注册抖音的方式有哪些"},
                        {"field_name": "答案", "field_value": "有手机号、邮箱等方式"},
                    ],
                }
            ],
        },
    }

    contexts = VolcengineKnowledgeSearchService()._extract_contexts(response)

    assert contexts == [
        (
            "切片类型: structured\n"
            "来源: 未命名表格.xlsx\n"
            "切片内容:\n"
            "问题:注册抖音的方式有哪些\n"
            "答案:有手机号、邮箱等方式\n"
            "结构化字段:\n"
            "问题: 注册抖音的方式有哪些\n"
            "答案: 有手机号、邮箱等方式"
        )
    ]


def test_volcengine_extract_contexts_prefers_higher_reranked_items() -> None:
    response = {
        "code": 0,
        "message": "success",
        "data": {
            "result_list": [
                {
                    "content": "低相关内容",
                    "chunk_type": "text",
                    "rerank_position": 2,
                    "rerank_score": 0.99,
                },
                {
                    "content": "高相关内容",
                    "chunk_type": "text",
                    "rerank_position": 1,
                    "rerank_score": 0.80,
                },
            ],
        },
    }

    contexts = VolcengineKnowledgeSearchService()._extract_contexts(response)

    assert contexts[0] == "切片类型: text\n切片内容:\n高相关内容"
