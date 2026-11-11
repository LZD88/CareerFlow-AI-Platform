import logging

from app.models.knowledge_base import KnowledgeSearchRequest
from app.services.knowledge_base.embedding_service import EmbeddingService
from app.services.knowledge_base.milvus_service import MilvusService

logger = logging.getLogger(__name__)


class KnowledgeSearchService:
    """
    知识库检索服务。

    当前 MVP 流程：
    1. 用户 query 调 embedding 生成 query vector；
    2. 根据 courseId/studyMode/documentId 组装 Milvus filter；
    3. Milvus search 返回 topK chunk；
    4. 转成接口友好的 camelCase 字段。
    """

    def __init__(self) -> None:
        self.embedding_service = EmbeddingService()
        self.milvus_service = MilvusService()

    def search(self, request: KnowledgeSearchRequest) -> dict:
        query = request.query.strip()
        if not query:
            raise ValueError("检索问题不能为空")

        query_vector = self.embedding_service.embed_text(query)
        filter_expr = self._build_filter(request)

        logger.info(
            "开始知识库检索: query_length=%s, top_k=%s, filter=%s",
            len(query),
            request.topK,
            filter_expr or "无",
        )
        hits = self.milvus_service.search_chunks(
            query_vector=query_vector,
            top_k=request.topK,
            filter_expr=filter_expr,
        )

        return {
            "ok": True,
            "query": query,
            "collection": self.milvus_service.settings.milvus_collection,
            "topK": request.topK,
            "resultCount": len(hits),
            "results": [self._to_response_hit(hit) for hit in hits],
        }

    def _build_filter(self, request: KnowledgeSearchRequest) -> str:
        filters: list[str] = []
        if request.courseId is not None:
            filters.append(f"course_id in [0, {request.courseId}]")
        else:
            filters.append("course_id == 0")
        if request.studyMode:
            filters.append(f'study_mode in ["ALL", "{self._escape_string(request.studyMode.upper())}"]')
        if request.documentId:
            filters.append(f'document_id == "{self._escape_string(request.documentId)}"')
        return " and ".join(filters)

    def _to_response_hit(self, hit: dict) -> dict:
        entity = hit.get("entity") or {}
        return {
            "id": str(hit.get("id") or entity.get("id") or ""),
            "score": float(hit.get("distance") or hit.get("score") or 0),
            "documentId": entity.get("document_id"),
            "chunkId": entity.get("chunk_id"),
            "courseId": entity.get("course_id"),
            "classId": entity.get("class_id"),
            "studyMode": entity.get("study_mode"),
            "title": entity.get("title"),
            "content": entity.get("content"),
            "version": entity.get("version"),
            "sourceType": entity.get("source_type"),
            "sourcePath": entity.get("source_path"),
            "knowledgeDomain": entity.get("knowledge_domain"),
            "chunkType": entity.get("chunk_type"),
            "methodName": entity.get("method_name"),
            "methodHeading": entity.get("method_heading"),
            "tableName": entity.get("table_name"),
        }

    def _escape_string(self, value: str) -> str:
        return value.replace("\\", "\\\\").replace('"', '\\"')
