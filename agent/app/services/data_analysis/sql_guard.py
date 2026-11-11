import re


class DataAnalysisSqlGuard:
    """
    Python 侧 SQL 基础兜底。

    最终执行前仍必须由 Java 后端再做强校验；这里的目标是避免 Agent 返回明显危险 SQL。
    """

    DANGEROUS_PATTERN = re.compile(
        r"\b(insert|update|delete|drop|alter|truncate|create|replace|grant|revoke|call|load|merge|execute)\b",
        re.IGNORECASE,
    )
    SYSTEM_SCHEMA_PATTERN = re.compile(
        r"\b(information_schema|mysql|performance_schema|sys)\b",
        re.IGNORECASE,
    )
    LIMIT_PATTERN = re.compile(r"\blimit\s+\d+", re.IGNORECASE)
    READONLY_HEAD_PATTERN = re.compile(r"^(select|with)\b", re.IGNORECASE)
    TABLE_PATTERN = re.compile(
        r"\b(?:from|join)\s+`?([a-zA-Z_][\w]*)`?(?:\s*\.\s*`?([a-zA-Z_][\w]*)`?)?",
        re.IGNORECASE,
    )
    CTE_PATTERN = re.compile(r"(?:with|,)\s+`?([a-zA-Z_][\w]*)`?\s+as\s*\(", re.IGNORECASE)

    def validate(self, sql: str, *, allowed_tables: list[str], max_rows: int = 100) -> str:
        value = self._normalize_sql(sql)
        if not value:
            raise ValueError("SQL 不能为空")

        if not self.READONLY_HEAD_PATTERN.match(value):
            raise ValueError("仅允许 SELECT/WITH 查询")
        if ";" in value:
            raise ValueError("禁止多语句 SQL")
        if self.DANGEROUS_PATTERN.search(value):
            raise ValueError("SQL 包含危险关键字")
        if self.SYSTEM_SCHEMA_PATTERN.search(value):
            raise ValueError("禁止访问系统库")

        self._validate_tables(value, allowed_tables)
        return self._ensure_limit(value, max_rows)

    def _normalize_sql(self, sql: str) -> str:
        value = (sql or "").strip()
        if value.startswith("```"):
            value = re.sub(r"^```(?:sql)?\s*", "", value, flags=re.IGNORECASE)
            value = re.sub(r"\s*```$", "", value)
        value = value.strip()
        if value.endswith(";"):
            value = value[:-1].strip()
        return value

    def _validate_tables(self, sql: str, allowed_tables: list[str]) -> None:
        allowed = {table.lower() for table in allowed_tables}
        cte_names = {match.group(1).lower() for match in self.CTE_PATTERN.finditer(sql)}
        found_tables: list[str] = []

        for match in self.TABLE_PATTERN.finditer(sql):
            first = match.group(1)
            second = match.group(2)
            table = second or first
            if table.lower() in cte_names:
                continue
            found_tables.append(table)

        if not found_tables:
            raise ValueError("SQL 未识别到可查询表")

        illegal_tables = [table for table in found_tables if table.lower() not in allowed]
        if illegal_tables:
            raise ValueError(f"SQL 包含未授权表: {', '.join(illegal_tables)}")

    def _ensure_limit(self, sql: str, max_rows: int) -> str:
        safe_max_rows = min(max(int(max_rows or 100), 1), 500)
        if self.LIMIT_PATTERN.search(sql):
            return sql
        return f"{sql} LIMIT {safe_max_rows}"
