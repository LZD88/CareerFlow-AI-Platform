import re


class DataAnalysisClarificationService:
    """Decide whether a data-analysis question needs clarification before SQL generation."""

    TIME_PATTERN = re.compile(
        r"(今天|昨日|昨天|本周|上周|本月|上月|本季度|上季度|今年|去年|近\d+\s*(天|日|周|月|年)|最近(\d+|一|两|二|三|四|五|六|七|八|九|十)?\s*个?\s*(天|日|周|月|年)|\d{4}[-/年]\d{1,2}|\d{4}年)"
    )
    MONEY_PATTERN = re.compile(r"(金额|销售额|收入|营收|GMV|付款|支付|成交|订单)")
    PAID_SCOPE_PATTERN = re.compile(r"(已支付|支付成功|已付款|全部订单|退款|未退款|净收入|实收)")
    TREND_PATTERN = re.compile(r"(趋势|变化|增长|下降|同比|环比|走势|按天|按日|按周|按月)")
    RANK_PATTERN = re.compile(r"(排名|排行|top|TOP|最高|最低|最多|最少|前\d+)")
    DIMENSION_PATTERN = re.compile(r"(每个|各|按|分校区|校区|班级|课程|学生|学员|商品|渠道|老师|月份|日期)")
    BROAD_ANALYSIS_PATTERN = re.compile(r"(情况|分析|看一下|怎么样|如何|概览|总览)$")

    def clarify_before_context(self, question: str) -> tuple[bool, list[str], str]:
        text = self._normalize(question)
        questions: list[str] = []

        if self._is_too_short(text):
            questions.append("你想分析哪个具体指标？例如订单金额、订单数量、学生人数、作业提交率等。")

        if self.BROAD_ANALYSIS_PATTERN.search(text) and len(text) <= 12:
            questions.append("请补充希望按什么维度拆分，或者希望看汇总、趋势还是排行。")

        questions = self._dedupe(questions)[:3]
        if not questions:
            return False, [], ""
        return True, questions, "问题过于宽泛，暂时无法选择可靠的数据分析口径。"

    def clarify_after_context(
        self,
        question: str,
        *,
        metric_contexts: list[str] | None = None,
        schema_contexts: list[str] | None = None,
    ) -> tuple[bool, list[str], str]:
        text = self._normalize(question)
        context = "\n".join((metric_contexts or []) + (schema_contexts or []))
        questions: list[str] = []

        has_time = bool(self.TIME_PATTERN.search(text))
        if not has_time and (self.MONEY_PATTERN.search(text) or self.TREND_PATTERN.search(text)):
            questions.append("请补充时间范围，例如最近7天、本月、上月或指定起止月份。")

        if (
            self.MONEY_PATTERN.search(text)
            and not self.PAID_SCOPE_PATTERN.search(text)
            and not self._has_default_money_scope(context)
        ):
            questions.append("金额口径按已支付订单、全部订单，还是扣除退款后的实收金额？")

        if self.RANK_PATTERN.search(text) and not self.DIMENSION_PATTERN.search(text):
            questions.append("请说明排行维度，例如按校区、班级、课程、商品或支付渠道排行。")

        questions = self._dedupe(questions)[:3]
        if not questions:
            return False, [], ""
        return True, questions, "已结合指标和表结构上下文判断，问题仍缺少生成可靠 SQL 所需的业务选择。"

    def clarify(self, question: str) -> tuple[bool, list[str], str]:
        return self.clarify_before_context(question)

    def _normalize(self, question: str) -> str:
        return re.sub(r"\s+", "", (question or "").strip())

    def _is_too_short(self, question: str) -> bool:
        if not question:
            return True
        return len(question) <= 4

    def _dedupe(self, values: list[str]) -> list[str]:
        result: list[str] = []
        for value in values:
            if value and value not in result:
                result.append(value)
        return result

    def _has_default_money_scope(self, context: str) -> bool:
        if not context:
            return False
        return bool(re.search(r"(默认过滤|计算口径|业务定义).*(已支付|支付成功|已付款|实收|退款)", context, re.S))
