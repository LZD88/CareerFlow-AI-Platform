import json

from app.models.knowledge_base import KnowledgeIngestTextRequest
from app.services.knowledge_base.knowledge_ingest_service import KnowledgeIngestService


class FakeMetricService:
    last_request = None

    def ingest_metrics(self, request):
        self.last_request = request
        return type("IngestResponse", (), {"collection": "data_analysis_metric_chunks", "count": len(request.metrics)})()

    def render_metric_content(self, metric):
        return f"指标：{metric.title}\n计算口径：{metric.formula}"


def test_ingest_text_routes_data_analysis_metric_json_to_metric_service() -> None:
    service = KnowledgeIngestService()
    service.metric_service = FakeMetricService()

    response = service.ingest_text(
        KnowledgeIngestTextRequest(
            documentId="metric_order_paid_amount",
            title="订单实收金额",
            content=json.dumps(
                {
                    "code": "order_paid_amount",
                    "title": "订单实收金额",
                    "type": "atomic",
                    "formula": "SUM(sale_order.paid_amount)",
                    "tables": ["sale_order"],
                },
                ensure_ascii=False,
            ),
            sourceType="data_analysis_metric_json",
            sourcePath="data-analysis/business-knowledge/metrics/order_paid_amount.json",
            knowledgeDomain="DATA_ANALYSIS_METRIC",
            version=2,
        )
    )

    assert response["collection"] == "data_analysis_metric_chunks"
    assert response["chunkCount"] == 1
    assert response["chunks"][0]["id"] == "metric_metric_order_paid_amount_v2_chunk_0001"
    assert response["chunks"][0]["chunkId"] == "chunk_0001"
    assert service.metric_service.last_request.documentId == "metric_order_paid_amount"
    assert service.metric_service.last_request.metrics[0].version == 2
