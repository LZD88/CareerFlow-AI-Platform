from app.services.data_analysis.clarification_service import DataAnalysisClarificationService


def test_clarification_service_asks_for_short_question() -> None:
    service = DataAnalysisClarificationService()

    needs_clarification, questions, reason = service.clarify_before_context("订单")

    assert needs_clarification is True
    assert questions
    assert reason


def test_clarification_service_does_not_ask_business_details_before_context() -> None:
    service = DataAnalysisClarificationService()

    needs_clarification, questions, reason = service.clarify_before_context("统计各校区订单金额")

    assert needs_clarification is False
    assert questions == []
    assert reason == ""


def test_clarification_service_asks_for_time_and_paid_scope_after_context() -> None:
    service = DataAnalysisClarificationService()

    needs_clarification, questions, _ = service.clarify_after_context("统计各校区订单金额")

    assert needs_clarification is True
    assert any("时间范围" in question for question in questions)
    assert any("金额口径" in question for question in questions)


def test_clarification_service_accepts_specific_paid_amount_question() -> None:
    service = DataAnalysisClarificationService()

    needs_clarification, questions, reason = service.clarify_after_context("统计最近一个月每个校区已支付订单金额")

    assert needs_clarification is False
    assert questions == []
    assert reason == ""


def test_clarification_service_asks_rank_dimension() -> None:
    service = DataAnalysisClarificationService()

    needs_clarification, questions, _ = service.clarify_after_context("最近一个月订单金额排名")

    assert needs_clarification is True
    assert any("排行维度" in question for question in questions)


def test_clarification_service_uses_context_default_money_scope() -> None:
    service = DataAnalysisClarificationService()

    needs_clarification, questions, _ = service.clarify_after_context(
        "统计本月各校区订单金额",
        metric_contexts=["计算口径：统计已支付订单金额。默认过滤：pay_status = PAID 表示支付成功。"],
    )

    assert needs_clarification is False
    assert questions == []
