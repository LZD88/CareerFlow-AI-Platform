import logging
import re

from app.models.data_analysis import (
    DataAnalysisMetricDefinition,
    DataAnalysisMetricIngestRequest,
    DataAnalysisMetricIngestResponse,
    DataAnalysisMetricItem,
    DataAnalysisMetricSearchRequest,
    DataAnalysisMetricSearchResponse,
)
from app.services.knowledge_base.embedding_service import EmbeddingService
from app.services.knowledge_base.milvus_service import MilvusService

logger = logging.getLogger(__name__)


class DataAnalysisMetricService:
    """数据分析业务指标口径知识库服务。"""

    KNOWLEDGE_DOMAIN = "DATA_ANALYSIS_METRIC"

    def __init__(self) -> None:
        self.embedding_service = EmbeddingService()
        self.milvus_service = MilvusService()

    def ingest_metrics(self, request: DataAnalysisMetricIngestRequest) -> DataAnalysisMetricIngestResponse:
        rows = []
        response_metrics: list[DataAnalysisMetricItem] = []
        collection = self.milvus_service.resolve_collection(self.KNOWLEDGE_DOMAIN)

        for index, metric in enumerate(request.metrics, start=1):
            content = self.render_metric_content(metric)
            vector = self.embedding_service.embed_text(content)
            chunk_id = f"chunk_{index:04d}"
            document_id = request.documentId.strip() or f"metric_{metric.code}"
            row_id = self._build_row_id(document_id, metric.version, index)
            tables = self._normalize_string_list(metric.tables)
            keywords = self._normalize_string_list(metric.keywords)
            rows.append(
                {
                    "id": row_id,
                    "vector": vector,
                    "document_id": document_id,
                    "chunk_id": chunk_id,
                    "course_id": 0,
                    "class_id": 0,
                    "study_mode": "ALL",
                    "permission_scope": "data_analysis_metric",
                    "title": metric.title,
                    "header_path": metric.title,
                    "content": content,
                    "source_type": "data_analysis_metric_json",
                    "source_path": request.sourcePath,
                    "version": metric.version,
                    "knowledge_domain": self.KNOWLEDGE_DOMAIN,
                    "chunk_type": "data_analysis_metric",
                    "metric_code": metric.code,
                    "metric_type": metric.type,
                    "metric_tables": ",".join(tables),
                    "keywords": ",".join(keywords),
                    "status": metric.status,
                }
            )
            response_metrics.append(
                DataAnalysisMetricItem(
                    code=metric.code,
                    title=metric.title,
                    type=metric.type,
                    content=content,
                    tables=tables,
                    documentId=document_id,
                    chunkId=chunk_id,
                    version=metric.version,
                )
            )

        self.milvus_service.upsert_chunks(rows, collection=collection)
        logger.info("数据分析指标入库完成: collection=%s, count=%s", collection, len(rows))
        return DataAnalysisMetricIngestResponse(
            ok=True,
            collection=collection,
            count=len(rows),
            metrics=response_metrics,
        )

    def search_metrics(self, request: DataAnalysisMetricSearchRequest) -> DataAnalysisMetricSearchResponse:
        question = request.question.strip()
        if not question:
            raise ValueError("检索问题不能为空")

        collection = self.milvus_service.resolve_collection(self.KNOWLEDGE_DOMAIN)
        query_vector = self.embedding_service.embed_text(question)
        hits = self.milvus_service.search_chunks(
            query_vector=query_vector,
            top_k=request.topK,
            filter_expr='knowledge_domain == "DATA_ANALYSIS_METRIC" and status == "ACTIVE"',
            collection=collection,
        )
        metrics = [self._to_metric_item(hit) for hit in hits]
        return DataAnalysisMetricSearchResponse(
            ok=True,
            question=question,
            collection=collection,
            resultCount=len(metrics),
            metrics=metrics,
        )

    def render_metric_content(self, metric: DataAnalysisMetricDefinition) -> str:
        """把结构化 JSON 指标渲染成适合 embedding/RAG/prompt 的自然语言文本。"""
        lines: list[str] = [
            f"指标：{metric.title}",
            f"指标编码：{metric.code}",
            f"指标类型：{metric.type}",
        ]
        if metric.keywords:
            lines.append(f"关键词：{'、'.join(metric.keywords)}")
        if metric.businessDefinition:
            lines.append(f"业务定义：{metric.businessDefinition}")
        if metric.formula:
            lines.append(f"计算口径：{metric.formula}")
        if metric.alternativeFormulas:
            lines.append("可选口径：")
            for item in metric.alternativeFormulas:
                name = str(item.get("name") or "").strip()
                formula = str(item.get("formula") or "").strip()
                if name or formula:
                    lines.append(f"- {name}：{formula}" if name else f"- {formula}")
        if metric.defaultFilters:
            lines.append("默认过滤：")
            lines.extend(f"- {item}" for item in metric.defaultFilters)
        if metric.defaultTimeField:
            lines.append(f"默认时间字段：{metric.defaultTimeField}")
        if metric.tables:
            lines.append(f"涉及表：{', '.join(metric.tables)}")
        if metric.commonDimensions:
            lines.append("常用维度：")
            for dimension in metric.commonDimensions:
                if not dimension.name and not dimension.field:
                    continue
                lines.append(f"- {dimension.name}：{dimension.field}")
                if dimension.joinPath:
                    lines.append(f"  关联链路：{'; '.join(dimension.joinPath)}")
        if metric.notes:
            lines.append("注意事项：")
            lines.extend(f"- {item}" for item in metric.notes)
        if metric.clarificationRules:
            lines.append("澄清规则：")
            for rule in metric.clarificationRules:
                if rule.when or rule.ask:
                    lines.append(f"- 当 {rule.when} 时，询问：{rule.ask}")
        if metric.examples:
            lines.append("示例问题：")
            lines.extend(f"- {item}" for item in metric.examples)
        return "\n".join(lines)

    def _to_metric_item(self, hit: dict) -> DataAnalysisMetricItem:
        entity = hit.get("entity") or {}
        return DataAnalysisMetricItem(
            code=str(entity.get("metric_code") or ""),
            title=str(entity.get("title") or ""),
            type=str(entity.get("metric_type") or ""),
            content=str(entity.get("content") or ""),
            tables=self._split_csv(str(entity.get("metric_tables") or "")),
            score=float(hit.get("distance") or hit.get("score") or 0),
            documentId=entity.get("document_id"),
            chunkId=entity.get("chunk_id"),
            version=entity.get("version"),
        )

    def _build_row_id(self, document_id: str, version: int, chunk_index: int) -> str:
        safe_document_id = re.sub(r"[^a-zA-Z0-9_-]+", "_", document_id).strip("_")[:80] or "metric"
        return f"metric_{safe_document_id}_v{version}_chunk_{chunk_index:04d}"

    def _normalize_string_list(self, value: list[str]) -> list[str]:
        result = []
        for item in value:
            text = str(item).strip()
            if text and text not in result:
                result.append(text)
        return result

    def _split_csv(self, value: str) -> list[str]:
        return [item.strip() for item in value.split(",") if item.strip()]
