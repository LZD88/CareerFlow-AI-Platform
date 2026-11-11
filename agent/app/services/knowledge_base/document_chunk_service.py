import logging
from app.models.knowledge_base import KnowledgeDeleteDocumentChunksRequest, KnowledgeDocumentChunksRequest
from app.services.knowledge_base.milvus_service import MilvusService

logger = logging.getLogger(__name__)


class DocumentChunkService:
    """
    从 Milvus 查询某份知识文档已经入库的 chunk。

    这个服务只做只读查询，给后台管理页排查切片内容、元数据和向量维度使用。
    """

    def __init__(self) -> None:
        self.milvus_service = MilvusService()

    def list_document_chunks(self, request: KnowledgeDocumentChunksRequest) -> dict:
        page_num = request.pageNum
        page_size = request.pageSize
        offset = (page_num - 1) * page_size
        filter_expr = self._build_filter(request)
        collection = self.milvus_service.resolve_collection(request.knowledgeDomain)

        # 使用 Milvus 的 offset/limit 做分页，只按 document_id/version 过滤。
        # 不依赖 chunk 主键生成规则，避免后续调整入库 ID 规则时影响详情查询。
        page_rows = self.milvus_service.query_chunks(
            filter_expr=filter_expr,
            include_vector=request.includeVector,
            limit=page_size,
            offset=offset,
            collection=collection,
        )
        page_rows = sorted(page_rows, key=self._chunk_sort_key)
        total = self.milvus_service.count_chunks(filter_expr, collection=collection)

        return {
            "ok": True,
            "documentId": request.documentId,
            "version": request.version,
            "collection": collection,
            "total": total,
            "pageNum": page_num,
            "pageSize": page_size,
            "records": [self._to_response_row(row, request.includeVector) for row in page_rows],
        }

    def delete_document_chunks(self, request: KnowledgeDeleteDocumentChunksRequest) -> dict:
        """
        删除某份文档在 Milvus 中的 chunk。

        更新知识库时 Java 会先入库新版本，成功后再调用这里删除旧版本，避免新版本失败导致知识库空窗。
        """
        filter_expr = self._build_delete_filter(request)
        collection = self.milvus_service.resolve_collection(request.knowledgeDomain)
        deleted_count = self.milvus_service.delete_chunks(filter_expr, collection=collection)
        return {
            "ok": True,
            "documentId": request.documentId,
            "version": request.version,
            "collection": collection,
            "deletedCount": deleted_count,
        }

    def _build_filter(self, request: KnowledgeDocumentChunksRequest) -> str:
        filters = [f'document_id == "{self._escape_string(request.documentId)}"']
        if request.version is not None:
            filters.append(f"version == {request.version}")
        return " and ".join(filters)

    def _build_delete_filter(self, request: KnowledgeDeleteDocumentChunksRequest) -> str:
        filters = [f'document_id == "{self._escape_string(request.documentId)}"']
        if request.version is not None:
            filters.append(f"version == {request.version}")
        return " and ".join(filters)

    def _to_response_row(self, row: dict, include_vector: bool) -> dict:
        vector = row.get("vector")
        if not isinstance(vector, list):
            vector = []
        content = str(row.get("content") or "")
        result = {
            "id": str(row.get("id") or ""),
            "documentId": row.get("document_id"),
            "chunkId": row.get("chunk_id"),
            "courseId": row.get("course_id"),
            "classId": row.get("class_id"),
            "studyMode": row.get("study_mode"),
            "permissionScope": row.get("permission_scope"),
            "title": row.get("title"),
            "headerPath": row.get("header_path"),
            "content": content,
            "contentLength": len(content),
            "version": row.get("version"),
            "sourceType": row.get("source_type"),
            "sourcePath": row.get("source_path"),
            "knowledgeDomain": row.get("knowledge_domain"),
            "chunkType": row.get("chunk_type"),
            "methodName": row.get("method_name"),
            "methodHeading": row.get("method_heading"),
            "tableName": row.get("table_name"),
            "vectorDim": len(vector),
            "vectorPreview": vector[:8],
        }
        if include_vector:
            result["vector"] = vector
        return result

    def _chunk_sort_key(self, row: dict) -> tuple[int, str]:
        chunk_id = str(row.get("chunk_id") or "")
        digits = "".join(ch for ch in chunk_id if ch.isdigit())
        return (int(digits) if digits else 0, chunk_id)

    def _escape_string(self, value: str) -> str:
        return value.replace("\\", "\\\\").replace('"', '\\"')
