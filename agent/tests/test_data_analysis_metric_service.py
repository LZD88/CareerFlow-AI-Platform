from app.models.data_analysis import DataAnalysisMetricDefinition
from app.services.data_analysis.metric_service import DataAnalysisMetricService


def test_render_metric_content_includes_business_contract() -> None:
    metric = DataAnalysisMetricDefinition(
        code="order_paid_amount",
        title="订单实收金额",
        type="atomic",
        keywords=["实收金额", "已收金额"],
        businessDefinition="统计订单维度已经实际支付的金额。",
        formula="SUM(sale_order.paid_amount)",
        defaultTimeField="sale_order.created_at",
        tables=["sale_order", "edu_class", "edu_campus"],
        commonDimensions=[
            {
                "name": "校区",
                "field": "edu_campus.campus_name",
                "joinPath": ["sale_order.class_id -> edu_class.id", "edu_class.campus_id -> edu_campus.id"],
            }
        ],
        notes=["用户说实收金额时，优先使用本指标。"],
        clarificationRules=[
            {
                "when": "用户同时提到支付成功和订单实收",
                "ask": "你要按支付流水金额统计，还是按订单累计实收金额统计？",
            }
        ],
        examples=["统计每个校区订单实收金额"],
    )

    content = DataAnalysisMetricService().render_metric_content(metric)

    assert "指标：订单实收金额" in content
    assert "计算口径：SUM(sale_order.paid_amount)" in content
    assert "默认时间字段：sale_order.created_at" in content
    assert "涉及表：sale_order, edu_class, edu_campus" in content
    assert "校区：edu_campus.campus_name" in content
    assert "用户同时提到支付成功和订单实收" in content
