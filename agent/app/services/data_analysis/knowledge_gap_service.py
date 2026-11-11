import re

from app.models.data_analysis import DataAnalysisSchemaItem


class DataAnalysisKnowledgeGapService:
    """Detect basic schema document gaps that should be fixed by administrators."""

    FORBIDDEN_FIELD_PATTERN = re.compile(r"(禁止查询字段|敏感字段|不允许展示)")

    def detect(self, question: str, schemas: list[DataAnalysisSchemaItem]) -> tuple[bool, list[str], str]:
        items: list[str] = []

        for schema in schemas:
            content = schema.content or ""
            fields = self._extract_fields(content)

            if not content.strip():
                items.append(f"{schema.table} 缺少表结构文档内容")
                continue
            if not fields:
                items.append(f"{schema.table} 缺少字段列表说明")
            if not self.FORBIDDEN_FIELD_PATTERN.search(content):
                items.append(f"{schema.table} 缺少字段安全审查说明，请补充“禁止查询字段”；如无敏感字段请填写“无”")

        items = self._dedupe(items)[:8]
        if not items:
            return False, [], ""
        return True, items, "候选表结构文档缺少基础说明，请先补充数据分析知识库。"

    def _extract_fields(self, content: str) -> list[str]:
        fields: list[str] = []
        for match in re.finditer(r"(?m)^-\s+([a-zA-Z_][\w]*):", content or ""):
            field = match.group(1).strip()
            if field and field not in fields:
                fields.append(field)
        for match in re.finditer(r"(?m)^\s*`([a-zA-Z_][\w]*)`\s+[a-zA-Z]", content or ""):
            field = match.group(1).strip()
            if field and field not in fields:
                fields.append(field)
        for match in re.finditer(r"(?m)^\s*([a-zA-Z_][\w]*)\s+(?:bigint|int|tinyint|varchar|char|decimal|datetime|timestamp|date|text|longtext|json)\b", content or "", re.IGNORECASE):
            field = match.group(1).strip()
            if field and field.lower() not in {"primary", "unique", "key", "index", "constraint"} and field not in fields:
                fields.append(field)
        return fields

    def _dedupe(self, values: list[str]) -> list[str]:
        result: list[str] = []
        for value in values:
            if value and value not in result:
                result.append(value)
        return result
