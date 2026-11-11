import logging
from typing import Any

from app.config.settings import get_settings

logger = logging.getLogger(__name__)


class MilvusService:
    """
    Milvus 向量数据库访问服务。

    当前阶段先只负责三件事：
    1. 检查 Python 服务能不能连上 Milvus；
    2. 创建学生知识库切片 collection；
    3. 插入并查询一条测试向量，验证 Milvus 链路真的可用。

    后面做文档入库时，文档切分、embedding 生成、RAG 检索都会复用这里的 client。
    """

    def __init__(self) -> None:
        self.settings = get_settings()

    def health(self) -> dict[str, Any]:
        """
        轻量连通性检查。

        这里不创建 collection，只读取当前 collection 列表。
        如果云服务器安全组、Milvus 端口、Docker 容器有问题，这个接口会最先暴露出来。
        """
        client = self._client()
        collections = client.list_collections(timeout=self.settings.milvus_timeout_s)
        return {
            "ok": True,
            "uri": self.settings.milvus_uri,
            "dbName": self.settings.milvus_db_name,
            "studentCollection": self.settings.milvus_collection,
            "codeCollection": self.settings.milvus_code_collection,
            "dataAnalysisCollection": self.settings.milvus_data_analysis_collection,
            "dataAnalysisMetricCollection": self.settings.milvus_data_analysis_metric_collection,
            "collections": collections,
        }

    def resolve_collection(self, knowledge_domain: str | None = None) -> str:
        """
        根据知识域选择 Milvus collection。

        STUDENT_QA 继续写学生知识库 collection；LOG_DIAGNOSIS 写代码知识库 collection；
        DATA_ANALYSIS_SCHEMA 写数据分析表结构 collection；
        DATA_ANALYSIS_METRIC 写数据分析业务指标口径 collection。
        不同检索场景物理隔离，不能只依赖动态字段过滤。
        """
        domain = (knowledge_domain or "").upper()
        if domain == "LOG_DIAGNOSIS":
            return self.settings.milvus_code_collection
        if domain == "DATA_ANALYSIS_SCHEMA":
            return self.settings.milvus_data_analysis_collection
        if domain == "DATA_ANALYSIS_METRIC":
            return self.settings.milvus_data_analysis_metric_collection
        return self.settings.milvus_collection

    def ensure_collection(self, collection: str | None = None) -> dict[str, Any]:
        """
        创建学生知识库切片 collection。

        第一版使用显式 schema 建表：
        - 主键：id，字符串，手动传入；
        - 向量字段：vector；
        - 维度：由 MILVUS_VECTOR_DIM 控制；
        - 距离：COSINE，更适合大多数文本 embedding。

        Milvus 对 VARCHAR 字段要求声明 max_length，所以这里不用快速建表接口，
        而是显式给 id 配置最大长度，避免启动时报 `type param(max_length)`。

        文档 ID、课程 ID、班级 ID、正文等元数据先通过动态字段保存。
        等 RAG 跑通后，如果需要更强过滤性能，再升级成显式 schema + scalar index。
        """
        client = self._client()
        collection = collection or self.settings.milvus_collection

        if not client.has_collection(collection, timeout=self.settings.milvus_timeout_s):
            schema = self._build_schema(client)
            index_params = client.prepare_index_params()
            index_params.add_index(
                field_name="vector",
                index_name="vector_index",
                index_type="AUTOINDEX",
                metric_type="COSINE",
            )

            client.create_collection(
                collection_name=collection,
                schema=schema,
                index_params=index_params,
                timeout=self.settings.milvus_timeout_s,
            )
            logger.info("Milvus collection 创建完成: collection=%s", collection)

        description = client.describe_collection(collection, timeout=self.settings.milvus_timeout_s)
        return {
            "ok": True,
            "createdOrExists": True,
            "collection": collection,
            "dimension": self.settings.milvus_vector_dim,
            "description": description,
        }

    def init_test(self) -> dict[str, Any]:
        """
        完整验证：建 collection -> 写入测试向量 -> 搜索测试向量。

        注意：这里写入的是固定测试数据，不是业务知识库数据。
        后面如果想清理，可以按 id 前缀 `test_chunk_` 删除。
        """
        self.ensure_collection()
        client = self._client()
        collection = self.settings.milvus_collection
        vector = self._test_vector()

        test_id = "test_chunk_001"
        client.upsert(
            collection_name=collection,
            timeout=self.settings.milvus_timeout_s,
            data=[
                {
                    "id": test_id,
                    "vector": vector,
                    "document_id": "test_document_001",
                    "chunk_id": "test_chunk_001",
                    "course_id": 0,
                    "class_id": 0,
                    "permission_scope": "dev",
                    "title": "Milvus 连接测试",
                    "content": "这是一条用于验证 Milvus 写入和检索链路的测试知识切片。",
                }
            ],
        )

        results = client.search(
            collection_name=collection,
            timeout=self.settings.milvus_timeout_s,
            data=[vector],
            limit=3,
            output_fields=[
                "document_id",
                "chunk_id",
                "course_id",
                "class_id",
                "permission_scope",
                "title",
                "content",
            ],
        )

        return {
            "ok": True,
            "collection": collection,
            "insertedId": test_id,
            "resultCount": len(results[0]) if results else 0,
            "results": results,
        }

    def upsert_chunks(self, rows: list[dict], collection: str | None = None) -> dict[str, Any]:
        """
        写入文档 chunk 向量。

        rows 必须包含：
        - id：业务主键
        - vector：embedding 向量
        其他字段会作为 dynamic field 写入 Milvus。
        """
        collection = collection or self.settings.milvus_collection
        if not rows:
            return {"ok": True, "collection": collection, "count": 0}

        self.ensure_collection(collection)
        client = self._client()
        client.upsert(collection_name=collection, data=rows, timeout=self.settings.milvus_timeout_s)
        return {
            "ok": True,
            "collection": collection,
            "count": len(rows),
        }

    def search_chunks(
        self,
        query_vector: list[float],
        top_k: int = 5,
        filter_expr: str = "",
        collection: str | None = None,
    ) -> list[dict]:
        """
        根据 query vector 检索最相似的知识 chunk。

        filter_expr 是 Milvus 标量过滤表达式，例如：
        - course_id == 3
        - course_id == 3 and class_id == 8
        """
        collection = collection or self.settings.milvus_collection
        self.ensure_collection(collection)
        client = self._client()
        kwargs: dict[str, Any] = {
            "collection_name": collection,
            "timeout": self.settings.milvus_timeout_s,
            "data": [query_vector],
            "limit": top_k,
            "output_fields": [
                "document_id",
                "chunk_id",
                "course_id",
                "class_id",
                "permission_scope",
                "title",
                "header_path",
                "content",
                "version",
                "source_type",
                "source_path",
                "knowledge_domain",
                "chunk_type",
                "method_name",
                "method_heading",
                "table_name",
                "metric_code",
                "metric_type",
                "metric_tables",
                "keywords",
                "status",
            ],
        }
        if filter_expr:
            kwargs["filter"] = filter_expr

        results = client.search(**kwargs)
        return list(results[0]) if results else []

    def query_chunks(
        self,
        filter_expr: str,
        include_vector: bool = False,
        limit: int = 100,
        offset: int = 0,
        collection: str | None = None,
    ) -> list[dict]:
        """
        按标量条件查询 chunk 明细。

        这里用于后台管理页查看某份文档在 Milvus 里的切片结果，不做向量相似度搜索。
        limit/offset 直接交给 Milvus 处理，避免 Python 全量拉取后再分页。
        """
        collection = collection or self.settings.milvus_collection
        self.ensure_collection(collection)
        client = self._client()
        output_fields = [
            "id",
            "document_id",
            "chunk_id",
            "course_id",
            "class_id",
            "study_mode",
            "permission_scope",
            "title",
            "header_path",
            "content",
            "version",
            "source_type",
            "source_path",
            "knowledge_domain",
            "chunk_type",
            "method_name",
            "method_heading",
            "table_name",
            "metric_code",
            "metric_type",
            "metric_tables",
            "keywords",
            "status",
            "vector",
        ]

        return client.query(
            collection_name=collection,
            timeout=self.settings.milvus_timeout_s,
            filter=filter_expr,
            output_fields=output_fields,
            limit=limit,
            offset=offset,
        )

    def count_chunks(self, filter_expr: str, collection: str | None = None) -> int:
        """
        统计符合条件的 chunk 数量。

        后台分页需要 total 表示完整结果集数量，不能用当前页记录数代替。
        """
        collection = collection or self.settings.milvus_collection
        self.ensure_collection(collection)
        client = self._client()
        rows = client.query(
            collection_name=collection,
            timeout=self.settings.milvus_timeout_s,
            filter=filter_expr,
            output_fields=["count(*)"],
        )
        if not rows:
            return 0
        value = rows[0].get("count(*)")
        return int(value or 0)

    def delete_chunks(self, filter_expr: str, collection: str | None = None) -> int:
        """
        按标量条件删除 chunk。

        主要用于知识文档更新：新版本成功入库后，删除旧版本的 Milvus chunk。
        """
        collection = collection or self.settings.milvus_collection
        self.ensure_collection(collection)
        client = self._client()
        result = client.delete(
            collection_name=collection,
            timeout=self.settings.milvus_timeout_s,
            filter=filter_expr,
        )
        if isinstance(result, dict):
            return int(result.get("delete_count") or result.get("deleteCount") or 0)
        delete_count = getattr(result, "delete_count", None)
        return int(delete_count or 0)

    def _client(self):
        """
        延迟导入 pymilvus。

        这样即使本地还没执行 `uv sync` 安装依赖，服务启动报错也会集中在 Milvus 接口，
        不会影响当前已经跑通的学生问答流式接口。
        """
        try:
            from pymilvus import MilvusClient
        except ImportError as ex:
            raise RuntimeError("缺少 pymilvus 依赖，请先执行 uv sync 或 uv add pymilvus") from ex

        token = self.settings.milvus_token or None
        return MilvusClient(
            uri=self.settings.milvus_uri,
            token=token,
            db_name=self.settings.milvus_db_name,
            timeout=self.settings.milvus_timeout_s,
        )

    def _build_schema(self, client):
        """
        构建学生知识库 collection 的基础 schema。

        这里先只固定两个核心字段：
        - id：业务侧生成的 chunk 主键；
        - vector：embedding 向量。

        其他字段通过 dynamic field 写入，便于第一版快速迭代。
        """
        from pymilvus import DataType

        schema = client.create_schema(
            auto_id=False,
            enable_dynamic_field=True,
            description="学生问答 RAG 知识切片向量集合",
        )
        schema.add_field(
            field_name="id",
            datatype=DataType.VARCHAR,
            is_primary=True,
            max_length=128,
        )
        schema.add_field(
            field_name="vector",
            datatype=DataType.FLOAT_VECTOR,
            dim=self.settings.milvus_vector_dim,
        )
        return schema

    def _test_vector(self) -> list[float]:
        """
        生成一个固定维度的测试向量。

        真实业务里这里会替换成 embedding 模型输出，例如 qwen/text-embedding-v4。
        当前只用于验证 Milvus 的 collection、insert、search 是否正常。
        """
        dim = self.settings.milvus_vector_dim
        return [1.0 if index == 0 else 0.0 for index in range(dim)]
