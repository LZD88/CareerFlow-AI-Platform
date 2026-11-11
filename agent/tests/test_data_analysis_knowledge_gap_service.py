from app.models.data_analysis import DataAnalysisSchemaItem
from app.services.data_analysis.knowledge_gap_service import DataAnalysisKnowledgeGapService


def test_knowledge_gap_accepts_basic_schema_document() -> None:
    service = DataAnalysisKnowledgeGapService()

    missing, items, reason = service.detect(
        "统计本月订单成功和失败情况",
        [
            DataAnalysisSchemaItem(
                table="sale_order",
                content="""
# sale_order
## 字段
- id: 订单ID
- order_status: 订单状态
- created_at: 创建时间
## 禁止查询字段
- phone: 手机号，敏感字段
## 常用口径
- 订单数 = COUNT(DISTINCT sale_order.id)
""",
            )
        ],
    )

    assert missing is False
    assert items == []
    assert reason == ""


def test_knowledge_gap_accepts_no_forbidden_fields_statement() -> None:
    service = DataAnalysisKnowledgeGapService()

    missing, items, reason = service.detect(
        "统计校区数量",
        [
            DataAnalysisSchemaItem(
                table="edu_campus",
                content="""
# edu_campus
## 字段
- id: 校区ID
- campus_name: 校区名称
## 禁止查询字段
- 无
## 常用口径
- 校区数 = COUNT(DISTINCT edu_campus.id)
""",
            )
        ],
    )

    assert missing is False
    assert items == []
    assert reason == ""


def test_knowledge_gap_detects_missing_forbidden_field_section() -> None:
    service = DataAnalysisKnowledgeGapService()

    missing, items, _ = service.detect(
        "统计本月学生人数",
        [
            DataAnalysisSchemaItem(
                table="edu_student",
                content="""
# edu_student
## 字段
- id: 学生ID
- created_at: 创建时间
## 常用口径
- 学员数 = COUNT(DISTINCT edu_student.id)
""",
            )
        ],
    )

    assert missing is True
    assert "edu_student 缺少字段安全审查说明，请补充“禁止查询字段”；如无敏感字段请填写“无”" in items


def test_knowledge_gap_detects_missing_field_list() -> None:
    service = DataAnalysisKnowledgeGapService()

    missing, items, _ = service.detect(
        "统计本月学生人数",
        [
            DataAnalysisSchemaItem(
                table="edu_student",
                content="""
# edu_student
## 禁止查询字段
- 无
""",
            )
        ],
    )

    assert missing is True
    assert "edu_student 缺少字段列表说明" in items


def test_knowledge_gap_accepts_mysql_ddl_fields() -> None:
    service = DataAnalysisKnowledgeGapService()

    missing, items, reason = service.detect(
        "统计本月订单数",
        [
            DataAnalysisSchemaItem(
                table="sale_order",
                content="""
CREATE TABLE `sale_order` (
  `id` bigint NOT NULL COMMENT '订单ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单编号',
  `order_status` varchar(32) DEFAULT NULL COMMENT '订单状态',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) COMMENT='订单表';

## 禁止查询字段
- phone: 手机号，敏感字段，默认禁止查询和展示
""",
            )
        ],
    )

    assert missing is False
    assert items == []
    assert reason == ""
