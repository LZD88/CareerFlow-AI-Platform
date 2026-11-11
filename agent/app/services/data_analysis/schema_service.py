from app.models.data_analysis import DataAnalysisSchemaRequest, DataAnalysisSchemaResponse, DataAnalysisSchemaItem
from app.services.knowledge_base.milvus_service import MilvusService


class DataAnalysisSchemaService:
    """
    Text-to-SQL 第二阶段：根据候选表名精确取回表结构文档。

    这里不做向量相似度检索。第一阶段已经得到 table name，因此直接按 Milvus 动态字段 table_name/title 精确过滤。
    """

    KNOWLEDGE_DOMAIN = "DATA_ANALYSIS_SCHEMA"
    MAX_TABLES = 20

    def __init__(self) -> None:
        self.milvus_service = MilvusService()

    def get_schemas(self, request: DataAnalysisSchemaRequest) -> DataAnalysisSchemaResponse:
        tables = self._normalize_tables(request.tables)
        if not tables:
            return DataAnalysisSchemaResponse(schemas=[], missingTables=[])

        filter_expr = self._build_filter(tables)
        collection = self.milvus_service.resolve_collection(self.KNOWLEDGE_DOMAIN)
        rows = self.milvus_service.query_chunks(
            filter_expr=filter_expr,
            include_vector=False,
            limit=max(len(tables) * 3, 10),
            offset=0,
            collection=collection,
        )

        by_table: dict[str, DataAnalysisSchemaItem] = {}
        for row in rows:
            table = str(row.get("table_name") or row.get("title") or "").strip()
            if table not in tables or table in by_table:
                continue
            by_table[table] = DataAnalysisSchemaItem(
                table=table,
                title=str(row.get("title") or table),
                content=str(row.get("content") or ""),
                documentId=row.get("document_id"),
                chunkId=row.get("chunk_id"),
                version=row.get("version"),
            )

        schemas = [by_table[table] for table in tables if table in by_table]
        missing_tables = [table for table in tables if table not in by_table]
        return DataAnalysisSchemaResponse(schemas=schemas, missingTables=missing_tables)

    def _normalize_tables(self, tables: list[str]) -> list[str]:
        result = []
        for table in tables[: self.MAX_TABLES]:
            value = table.strip()
            if value and value not in result:
                result.append(value)
        return result

    def _build_filter(self, tables: list[str]) -> str:
        values = ", ".join(f'"{self._escape_string(table)}"' for table in tables)
        return f'(table_name in [{values}] or title in [{values}])'

    def _escape_string(self, value: str) -> str:
        return value.replace("\\", "\\\\").replace('"', '\\"')
