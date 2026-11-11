import json
import logging
from typing import Any

from langchain_openai import ChatOpenAI

from app.config.settings import get_settings
from app.models.data_analysis import (
    DataAnalysisChartSuggestion,
    DataAnalysisInterpretResultRequest,
    DataAnalysisInterpretResultResponse,
)

logger = logging.getLogger(__name__)


class DataAnalysisResultInterpreter:
    """解释 Java 已查询出的数据结果，不连接数据库。"""

    MAX_ROWS_FOR_PROMPT = 50
    MAX_CELL_TEXT_LENGTH = 500

    def __init__(self) -> None:
        self.settings = get_settings()

    def interpret(self, request: DataAnalysisInterpretResultRequest) -> DataAnalysisInterpretResultResponse:
        llm = self._build_llm()
        prompt = self._build_prompt(request)
        logger.info(
            "开始解释数据分析结果: question=%s, row_count=%s, prompt_length=%s, model=%s",
            request.question,
            request.rowCount,
            len(prompt),
            self.settings.qwen_data_analysis_interpret_model or self.settings.qwen_router_model or self.settings.qwen_model,
        )
        response = llm.invoke(prompt)
        content = (
            response.content
            if isinstance(response.content, str)
            else json.dumps(response.content, ensure_ascii=False)
        )
        logger.info("数据分析结果解释模型原始返回: %s", content)
        data = self._parse_json_response(content)
        chart_suggestion = self._parse_chart_suggestion(data.get("chartSuggestion") or data.get("chart_suggestion"))
        return DataAnalysisInterpretResultResponse(
            analysis=str(data.get("analysis") or "").strip(),
            insights=self._normalize_string_list(data.get("insights")),
            followUpQuestions=self._normalize_string_list(
                data.get("followUpQuestions") or data.get("follow_up_questions")
            ),
            chartSuggestion=chart_suggestion,
        )

    def _build_llm(self) -> ChatOpenAI:
        if self.settings.qwen_api_key:
            return ChatOpenAI(
                api_key=self.settings.qwen_api_key,
                base_url=self.settings.qwen_base_url,
                model=self.settings.qwen_data_analysis_interpret_model or self.settings.qwen_router_model or self.settings.qwen_model,
                temperature=0.1,
                max_tokens=self.settings.qwen_data_analysis_interpret_max_tokens,
                extra_body={"enable_thinking": False}
            )
        if self.settings.ark_api_key:
            return ChatOpenAI(
                api_key=self.settings.ark_api_key,
                base_url=self.settings.ark_base_url,
                model=self.settings.ark_model,
                temperature=0.1,
            )
        raise ValueError("QWEN_API_KEY/ARK_API_KEY 未配置，无法调用大模型")

    def _build_prompt(self, request: DataAnalysisInterpretResultRequest) -> str:
        payload = {
            "question": request.question,
            "sql": request.sql,
            "columns": request.columns,
            "rows": self._compact_rows(request.rows),
            "rowCount": request.rowCount,
            "truncated": request.truncated,
            "assumptions": request.assumptions,
            "warnings": request.warnings,
        }
        return f"""你是业务数据分析助手。
请基于 Java 后端已经查询出的 SQL 结果，给出简洁、可信的中文分析。

规则：
1. 只能基于输入的 rows、rowCount、columns、sql 和 assumptions 分析，禁止编造数据。
2. 如果 rows 为空，要说明当前查询未返回数据，并给出可能的业务排查方向。
3. 如果 truncated=true，要明确说明分析只基于当前样本或截断结果。
4. insights 输出 1 到 5 条关键洞察，必须包含具体字段或数值依据。
5. followUpQuestions 输出 0 到 3 个可继续追问的问题。
6. chartSuggestion 可为空；如果推荐图表，字段必须来自 columns。
7. 只返回 JSON，不要输出 Markdown 或额外解释。

输出格式：
{{
  "analysis": "",
  "insights": [],
  "followUpQuestions": [],
  "chartSuggestion": {{
    "chartType": "",
    "xField": "",
    "yField": "",
    "seriesField": "",
    "reason": ""
  }}
}}

输入数据：
{json.dumps(payload, ensure_ascii=False, default=str)}
"""

    def _compact_rows(self, rows: list[dict]) -> list[dict]:
        compact_rows: list[dict] = []
        for row in rows[: self.MAX_ROWS_FOR_PROMPT]:
            compact_row: dict[str, Any] = {}
            for key, value in row.items():
                if isinstance(value, str) and len(value) > self.MAX_CELL_TEXT_LENGTH:
                    compact_row[key] = value[: self.MAX_CELL_TEXT_LENGTH] + "...(truncated)"
                else:
                    compact_row[key] = value
            compact_rows.append(compact_row)
        return compact_rows

    def _parse_json_response(self, content: str) -> dict:
        text = content.strip()
        start = text.find("{")
        end = text.rfind("}")
        if start >= 0 and end > start:
            text = text[start:end + 1]
        data = json.loads(text)
        if not isinstance(data, dict):
            raise ValueError("模型返回不是 JSON 对象")
        return data

    def _normalize_string_list(self, value) -> list[str]:
        if not isinstance(value, list):
            return []
        result = []
        for item in value:
            text = str(item).strip()
            if text:
                result.append(text)
        return result

    def _parse_chart_suggestion(self, value) -> DataAnalysisChartSuggestion | None:
        if not isinstance(value, dict):
            return None
        chart_type = str(value.get("chartType") or value.get("chart_type") or "").strip()
        if not chart_type:
            return None
        return DataAnalysisChartSuggestion(
            chartType=chart_type,
            xField=str(value.get("xField") or value.get("x_field") or "").strip(),
            yField=str(value.get("yField") or value.get("y_field") or "").strip(),
            seriesField=str(value.get("seriesField") or value.get("series_field") or "").strip(),
            reason=str(value.get("reason") or "").strip(),
        )
