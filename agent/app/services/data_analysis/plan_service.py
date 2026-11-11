import json
import logging
from pathlib import Path

from langchain_openai import ChatOpenAI

from app.config.settings import get_settings
from app.models.data_analysis import (
    DataAnalysisPlanRequest,
    DataAnalysisPlanResponse,
    DataAnalysisTableCatalogItem,
)

logger = logging.getLogger(__name__)


class DataAnalysisPlanService:
    """
    Text-to-SQL 第一阶段：根据用户问题和轻量表目录选择候选表。

    这一阶段不生成 SQL、不查详细 schema、不访问业务数据库，只负责规划需要哪些表。
    """

    MAX_TABLE_CATALOG_CHARS = 30000
    TABLE_CATALOG_PATH = Path(__file__).resolve().parents[2] / "resources" / "data-analysis" / "table-catalog.json"

    def __init__(self) -> None:
        self.settings = get_settings()

    def plan(self, request: DataAnalysisPlanRequest) -> DataAnalysisPlanResponse:
        table_catalog = request.tableCatalog or self._load_default_table_catalog()
        if not table_catalog:
            raise ValueError("tableCatalog 不能为空")

        llm = self._build_llm()
        prompt = self._build_prompt(request.question, table_catalog)
        logger.info(
            "开始数据分析选表: question=%s, table_count=%s, prompt_length=%s",
            request.question,
            len(table_catalog),
            len(prompt),
        )

        response = llm.invoke(prompt)
        content = (
            response.content
            if isinstance(response.content, str)
            else json.dumps(response.content, ensure_ascii=False)
        )
        logger.info("数据分析选表模型原始返回: %s", content)
        data = self._parse_json_response(content)

        allowed_tables = {item.table for item in table_catalog}
        candidate_tables = []
        for table in data.get("candidateTables") or data.get("candidate_tables") or []:
            if isinstance(table, str) and table in allowed_tables and table not in candidate_tables:
                candidate_tables.append(table)

        return DataAnalysisPlanResponse(
            candidateTables=candidate_tables,
            reason=str(data.get("reason") or ""),
        )

    def _build_llm(self) -> ChatOpenAI:
        if self.settings.qwen_api_key:
            return ChatOpenAI(
                api_key=self.settings.qwen_api_key,
                base_url=self.settings.qwen_base_url,
                model=self.settings.qwen_router_model or self.settings.qwen_model,
                temperature=0,
                extra_body={"enable_thinking": False},
            )
        if self.settings.ark_api_key:
            return ChatOpenAI(
                api_key=self.settings.ark_api_key,
                base_url=self.settings.ark_base_url,
                model=self.settings.ark_model,
                temperature=0,
            )
        raise ValueError("QWEN_API_KEY/ARK_API_KEY 未配置，无法调用大模型")

    def _load_default_table_catalog(self) -> list:
        if not self.TABLE_CATALOG_PATH.exists():
            raise ValueError(f"默认表目录不存在: {self.TABLE_CATALOG_PATH}")
        data = json.loads(self.TABLE_CATALOG_PATH.read_text(encoding="utf-8"))
        if not isinstance(data, list):
            raise ValueError("默认表目录必须是 JSON 数组")
        return [DataAnalysisTableCatalogItem(**item) for item in data]

    def _build_prompt(self, question: str, table_catalog: list) -> str:
        catalog_json = json.dumps(
            [item.model_dump(exclude_none=True) for item in table_catalog],
            ensure_ascii=False,
        )
        if len(catalog_json) > self.MAX_TABLE_CATALOG_CHARS:
            catalog_json = catalog_json[: self.MAX_TABLE_CATALOG_CHARS] + "...(truncated)"

        return f"""你是数据分析选表助手。
根据用户问题和表目录，选择后续生成 SQL 所需的候选表。

规则：
1. 只能从表目录中选择表，禁止编造表名。
2. 如果需要跨表统计，必须选择关联链路上的中间表。
3. 只做选表，不要生成 SQL。
4. candidateTables 控制在 1 到 8 张表。
5. 只返回 JSON，不要输出 Markdown 或额外解释。

输出格式：
{{
  "candidateTables": [],
  "reason": ""
}}

用户问题：
{question}

表目录：
{catalog_json}
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
