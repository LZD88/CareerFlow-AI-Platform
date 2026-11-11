import json
import logging

from langchain_openai import ChatOpenAI

from app.config.settings import get_settings
from app.models.data_analysis import DataAnalysisMetricItem, DataAnalysisSchemaItem, DataAnalysisSqlPlan

logger = logging.getLogger(__name__)


class DataAnalysisSqlGenerationService:
    """根据用户问题和精确召回的 DDL 生成 MySQL SELECT 查询。"""

    MAX_SCHEMA_CHARS = 50000

    def __init__(self) -> None:
        self.settings = get_settings()

    def generate(
        self,
        *,
        question: str,
        candidate_tables: list[str],
        schemas: list[DataAnalysisSchemaItem],
        max_rows: int,
        metrics: list[DataAnalysisMetricItem] | None = None,
    ) -> DataAnalysisSqlPlan:
        llm = self._build_llm()
        prompt = self._build_prompt(
            question=question,
            candidate_tables=candidate_tables,
            schemas=schemas,
            metrics=metrics or [],
            max_rows=max_rows,
        )
        logger.info(
            "开始生成数据分析 SQL: question=%s, candidate_tables=%s, schema_count=%s, prompt_length=%s, model=%s",
            question,
            candidate_tables,
            len(schemas),
            len(prompt),
            self.settings.qwen_data_analysis_sql_model or self.settings.qwen_router_model or self.settings.qwen_model,
        )
        response = llm.invoke(prompt)
        content = (
            response.content
            if isinstance(response.content, str)
            else json.dumps(response.content, ensure_ascii=False)
        )
        logger.info("数据分析 SQL 模型原始返回: %s", content)
        data = self._parse_json_response(content)
        return DataAnalysisSqlPlan(
            sql=str(data.get("sql") or "").strip(),
            reason=str(data.get("reason") or data.get("explanation") or "").strip(),
            usedTables=self._normalize_string_list(data.get("usedTables") or data.get("used_tables")),
            assumptions=self._normalize_string_list(data.get("assumptions")),
            warnings=self._normalize_string_list(data.get("warnings") or data.get("riskWarnings") or data.get("risk_warnings")),
        )

    def _build_llm(self) -> ChatOpenAI:
        if self.settings.qwen_api_key:
            return ChatOpenAI(
                api_key=self.settings.qwen_api_key,
                base_url=self.settings.qwen_base_url,
                model=self.settings.qwen_data_analysis_sql_model or self.settings.qwen_router_model or self.settings.qwen_model,
                temperature=0,
                max_tokens=self.settings.qwen_data_analysis_sql_max_tokens,
                extra_body={"enable_thinking": False}
            )
        if self.settings.ark_api_key:
            return ChatOpenAI(
                api_key=self.settings.ark_api_key,
                base_url=self.settings.ark_base_url,
                model=self.settings.ark_model,
                temperature=0,
            )
        raise ValueError("QWEN_API_KEY/ARK_API_KEY 未配置，无法调用大模型")

    def _build_prompt(
        self,
        *,
        question: str,
        candidate_tables: list[str],
        schemas: list[DataAnalysisSchemaItem],
        metrics: list[DataAnalysisMetricItem],
        max_rows: int,
    ) -> str:
        metric_context = "\n\n".join(
            f"指标编码: {metric.code}\n指标名称: {metric.title}\n涉及表: {', '.join(metric.tables)}\n口径说明:\n{metric.content.strip()}"
            for metric in metrics
            if metric.content.strip()
        )
        schema_context = "\n\n".join(
            f"表名: {schema.table}\nDDL:\n{schema.content.strip()}" for schema in schemas
        )
        if len(schema_context) > self.MAX_SCHEMA_CHARS:
            schema_context = schema_context[: self.MAX_SCHEMA_CHARS] + "\n...(truncated)"

        return f"""你是 MySQL 数据分析 SQL 生成助手。
请根据用户问题和提供的 DDL 生成一条可执行的只读 SQL。

规则：
1. 只能生成 SELECT 或 WITH 查询。
2. 只能使用候选表和提供的 DDL，禁止编造表名、字段名。
3. 禁止 INSERT、UPDATE、DELETE、DROP、ALTER、TRUNCATE、CREATE、REPLACE、CALL、LOAD。
4. 如果问题需要时间范围但用户未说明，可以做合理假设，并写入 assumptions。
5. SQL 必须适配 MySQL。
6. 如果无法可靠生成 SQL，sql 返回空字符串，并在 warnings 中说明原因。
7. 如果 SQL 没有 LIMIT，必须追加 LIMIT {max_rows}。
8. 如果命中了业务指标口径，必须优先遵守指标的计算口径、默认过滤、默认时间字段、关联链路和注意事项。
9. 如果用户问题和业务指标口径冲突，优先在 warnings 中说明，不要自行改写业务口径。
10. 只返回 JSON，不要输出 Markdown 或额外解释。

输出格式：
{{
  "sql": "",
  "reason": "",
  "usedTables": [],
  "assumptions": [],
  "warnings": []
}}

用户问题：
{question}

候选表：
{json.dumps(candidate_tables, ensure_ascii=False)}

业务指标口径：
{metric_context or "无"}

表结构 DDL：
{schema_context}
"""

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
