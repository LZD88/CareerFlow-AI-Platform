import logging
import json
import re

from app.models.data_analysis import DataAnalysisMetricDefinition, DataAnalysisMetricIngestRequest
from app.models.knowledge_base import KnowledgeIngestTextRequest
from app.services.data_analysis.metric_service import DataAnalysisMetricService
from app.services.knowledge_base.chunk_service import ChunkService
from app.services.knowledge_base.embedding_service import EmbeddingService
from app.services.knowledge_base.milvus_service import MilvusService

logger = logging.getLogger(__name__)


class KnowledgeIngestService:
    """
    知识库文本入库服务。

    当前流程：
    1. 接收一段文本；
    2. 用 LlamaIndex 切成 Node；
       - Markdown 文档会先按标题层级切章节；
       - 章节过长时再按句子继续切 chunk；
       - 普通文本直接按句子/段落切 chunk；
    3. 每个 chunk 调火山 embedding；
    4. 写入 Milvus。

    后面 Java 管理页面上传文件后，也会复用这条服务链路。
    """

    def __init__(self) -> None:
        self.chunk_service = ChunkService()
        self.embedding_service = EmbeddingService()
        self.milvus_service = MilvusService()
        self.metric_service = DataAnalysisMetricService()

    def ingest_text(self, request: KnowledgeIngestTextRequest) -> dict:
        if self._is_data_analysis_metric_request(request):
            return self._ingest_data_analysis_metrics(request)

        nodes = self.chunk_service.split_nodes(
            request.content,
            source_type=request.sourceType,
            metadata={
                "document_id": request.documentId,
                "title": request.title,
                "course_id": request.courseId or 0,
                "study_mode": request.studyMode or "ALL",
                "source_type": request.sourceType,
                "source_path": request.sourcePath,
                "version": request.version,
                "knowledge_domain": request.knowledgeDomain or "STUDENT_QA",
            },
        )
        if not nodes:
            raise ValueError("文档内容为空，无法入库")

        logger.info(
            "开始知识文本入库: document_id=%s, version=%s, chunk_count=%s",
            request.documentId,
            request.version,
            len(nodes),
        )

        rows = []
        chunk_results = []
        for index, node in enumerate(nodes, start=1):
            chunk = node.get_content().strip()
            if not chunk:
                continue
            chunk_id = f"chunk_{index:04d}"
            row_id = self._build_row_id(request.documentId, request.version, index)

            vector = self.embedding_service.embed_text(chunk)
            header_path = str((node.metadata or {}).get("header_path") or "").strip()
            # generated_java_md 会在 ChunkService 中按方法段落切片，并把方法信息放到 metadata。
            chunk_type = str((node.metadata or {}).get("chunk_type") or "").strip()
            method_name = str((node.metadata or {}).get("method_name") or "").strip()
            method_heading = str((node.metadata or {}).get("method_heading") or "").strip()
            table_name = str((node.metadata or {}).get("table_name") or "").strip()
            rows.append(
                {
                    "id": row_id,
                    "vector": vector,
                    "document_id": request.documentId,
                    "chunk_id": chunk_id,
                    "course_id": request.courseId or 0,
                    "class_id": 0,
                    "study_mode": request.studyMode or "ALL",
                    "permission_scope": request.permissionScope,
                    "title": request.title,
                    "header_path": header_path,
                    "content": chunk,
                    "source_type": request.sourceType,
                    "source_path": request.sourcePath,
                    "version": request.version,
                    # 动态字段：第一版不固定 Milvus schema，后续可按检索需求升级 scalar index。
                    "knowledge_domain": request.knowledgeDomain or "STUDENT_QA",
                    "chunk_type": chunk_type,
                    "method_name": method_name,
                    "method_heading": method_heading,
                    "table_name": table_name,
                }
            )
            chunk_results.append(
                {
                    "id": row_id,
                    "chunkId": chunk_id,
                    "contentLength": len(chunk),
                }
            )

        collection = self.milvus_service.resolve_collection(request.knowledgeDomain)
        self.milvus_service.upsert_chunks(rows, collection=collection)
        logger.info(
            "知识文本入库完成: document_id=%s, version=%s, chunk_count=%s",
            request.documentId,
            request.version,
            len(rows),
        )
        return {
            "ok": True,
            "documentId": request.documentId,
            "version": request.version,
            "collection": collection,
            "chunkCount": len(rows),
            "chunks": chunk_results,
        }

    def _build_row_id(self, document_id: str, version: int, chunk_index: int) -> str:
        safe_document_id = re.sub(r"[^a-zA-Z0-9_-]+", "_", document_id).strip("_")
        safe_document_id = safe_document_id[:80] or "document"
        return f"doc_{safe_document_id}_v{version}_chunk_{chunk_index:04d}"

    def _is_data_analysis_metric_request(self, request: KnowledgeIngestTextRequest) -> bool:
        return (request.knowledgeDomain or "").upper() == "DATA_ANALYSIS_METRIC"

    def _ingest_data_analysis_metrics(self, request: KnowledgeIngestTextRequest) -> dict:
        metrics = self._parse_metric_definitions(request.content, default_version=request.version)
        ingest_response = self.metric_service.ingest_metrics(
            DataAnalysisMetricIngestRequest(
                metrics=metrics,
                documentId=request.documentId,
                title=request.title,
                sourcePath=request.sourcePath,
            )
        )
        chunks = []
        for index, metric in enumerate(metrics, start=1):
            content = self.metric_service.render_metric_content(metric)
            chunks.append(
                {
                    "id": self._build_metric_row_id(request.documentId, metric.version, index),
                    "chunkId": f"chunk_{index:04d}",
                    "contentLength": len(content),
                }
            )
        return {
            "ok": True,
            "documentId": request.documentId,
            "version": request.version,
            "collection": ingest_response.collection,
            "chunkCount": ingest_response.count,
            "chunks": chunks,
        }

    def _parse_metric_definitions(self, content: str, *, default_version: int) -> list[DataAnalysisMetricDefinition]:
        try:
            payload = json.loads(content)
        except json.JSONDecodeError as ex:
            raise ValueError("DATA_ANALYSIS_METRIC 入库内容必须是合法 JSON") from ex

        if isinstance(payload, dict) and isinstance(payload.get("metrics"), list):
            items = payload["metrics"]
        elif isinstance(payload, list):
            items = payload
        elif isinstance(payload, dict):
            items = [payload]
        else:
            raise ValueError("DATA_ANALYSIS_METRIC 入库内容必须是指标对象、指标数组或包含 metrics 数组的对象")

        metrics = []
        for item in items:
            if not isinstance(item, dict):
                raise ValueError("指标定义必须是 JSON 对象")
            item = dict(item)
            # 这里使用外层知识文档版本，而不是指标 JSON 内部固定写死的 version。
            # 否则知识库管理页“重入库”后，Java 会按新版本查 chunk，但 Milvus 里仍落在旧版本上。
            item["version"] = default_version
            metrics.append(DataAnalysisMetricDefinition(**item))
        return metrics

    def _build_metric_row_id(self, document_id: str, version: int, chunk_index: int) -> str:
        safe_document_id = re.sub(r"[^a-zA-Z0-9_-]+", "_", document_id).strip("_")[:80] or "metric"
        return f"metric_{safe_document_id}_v{version}_chunk_{chunk_index:04d}"
