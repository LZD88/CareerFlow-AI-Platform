from app.models.diagnose import DiagnoseLogRequest
from app.services.log_diagnosis.code_knowledge_search_service import CodeKnowledgeSearchService
from app.services.log_diagnosis.volcengine_knowledge_search_service import VolcengineKnowledgeSearchService


class RagService:
    """
    日志诊断 RAG 门面。

    DiagnoseService 只依赖这个稳定入口；当前默认走 Milvus 代码知识库。
    火山引擎知识库实现仍保留在 VolcengineKnowledgeSearchService，方便回滚或对比召回效果。
    """

    def __init__(self) -> None:
        self.code_knowledge_search_service = CodeKnowledgeSearchService()
        self.volcengine_knowledge_search_service = VolcengineKnowledgeSearchService()

    def retrieve(self, request: DiagnoseLogRequest) -> list[str]:
        return self.code_knowledge_search_service.retrieve(request)

    def retrieve_from_volcengine(self, request: DiagnoseLogRequest) -> list[str]:
        return self.volcengine_knowledge_search_service.retrieve(request)
