import logging
import re
from typing import Callable

from llama_index.core.schema import NodeWithScore

from app.config.settings import get_settings
from app.models.diagnose import DiagnoseLogRequest
from app.services.knowledge_base.qwen_rerank_service import QwenRerankService
from app.services.log_diagnosis.llama_index_code_retriever import LlamaIndexCodeKnowledgeRetriever

logger = logging.getLogger(__name__)


class CodeKnowledgeSearchService:
    """
    日志诊断代码知识库检索服务。

    这里专门查询 LOG_DIAGNOSIS 对应的 Milvus collection，不再调用火山引擎知识库。
    代码知识库的 chunk 来自 generated-java Markdown，每个 chunk 通常对应一个 Java 方法。
    """

    DEFAULT_TOP_K = 5

    def __init__(
        self,
        retriever_factory: Callable[[int], LlamaIndexCodeKnowledgeRetriever] | None = None,
    ) -> None:
        self.settings = get_settings()
        self.rerank_service = QwenRerankService()
        self.retriever_factory = retriever_factory or (
            lambda top_k: LlamaIndexCodeKnowledgeRetriever(top_k=top_k)
        )

    def retrieve(self, request: DiagnoseLogRequest, top_k: int | None = None) -> list[str]:
        query = self.build_query(request)
        if not query.strip():
            logger.info("日志诊断缺少有效检索字段，跳过代码知识库检索: guid=%s", request.guid)
            return []

        recall_top_k = top_k or self._get_recall_top_k()

        logger.info(
            "开始检索代码知识库: guid=%s, retriever=%s, top_k=%s, query_length=%s",
            request.guid,
            "LlamaIndexCodeKnowledgeRetriever",
            recall_top_k,
            len(query),
        )

        try:
            retriever = self.retriever_factory(recall_top_k)
            nodes = retriever.retrieve(query)
            nodes = self._rerank_nodes(query, nodes)
        except Exception:
            logger.exception("代码知识库 LlamaIndex 检索失败: guid=%s", request.guid)
            return []

        contexts = [self._format_node(node) for node in nodes[: self.DEFAULT_TOP_K]]
        contexts = [context for context in contexts if context]
        logger.info("代码知识库检索完成: guid=%s, context_count=%s", request.guid, len(contexts))
        return contexts

    def _get_recall_top_k(self) -> int:
        if self.settings.qwen_rerank_enabled:
            return max(self.settings.qwen_rerank_top_n, self.settings.qwen_rerank_recall_top_k)
        return self.DEFAULT_TOP_K

    def _rerank_nodes(self, query: str, nodes: list[NodeWithScore]) -> list[NodeWithScore]:
        if not nodes:
            return []
        if not self.settings.qwen_rerank_enabled:
            return nodes[: self.DEFAULT_TOP_K]
        return self.rerank_service.rerank(
            query=query,
            nodes=nodes,
            top_n=self.settings.qwen_rerank_top_n,
        )

    def build_query(self, request: DiagnoseLogRequest) -> str:
        """
        把日志结构化字段拼成向量检索 query。

        重点放在异常、堆栈、接口、控制器方法上，这些字段最容易命中代码方法 chunk。
        """
        parts = [
            "Java 日志诊断，检索相关代码方法、异常出口、日志语句和依赖调用。",
            f"请求路径: {request.request_uri or '未知'}",
            f"HTTP方法: {request.http_method or '未知'}",
            f"控制器: {request.controller or '未知'}",
            f"请求参数: {self._format_request_args_for_query(request.request_args)}",
            f"异常类型: {request.exception_class or '未知'}",
            f"异常信息: {request.exception_message or '未知'}",
            f"首个堆栈: {request.top_stack_frame or '未知'}",
        ]

        stack_class, stack_method, stack_line = self._parse_stack_frame(request.top_stack_frame or "")
        if stack_class:
            parts.append(f"堆栈类: {stack_class}")
        if stack_method:
            parts.append(f"堆栈方法: {stack_method}")
        if stack_line:
            parts.append(f"堆栈行号: {stack_line}")
        if request.raw_logs:
            parts.append("原始日志片段: " + "\n".join(line.strip()[:500] for line in request.raw_logs[:2] if line.strip()))
        return "\n".join(parts)

    def _format_request_args_for_query(self, request_args: str | None) -> str:
        if not request_args or not request_args.strip():
            return "未知"
        text = request_args.strip()
        return text[:800] + ("...(truncated)" if len(text) > 800 else "")

    def _format_node(self, node_with_score: NodeWithScore) -> str:
        node = node_with_score.node
        metadata = node.metadata or {}
        content = node.get_content().strip()
        if not content:
            return ""

        score = float(node_with_score.score or 0)
        lines = [
            f"来源: {metadata.get('source_path') or metadata.get('title') or '未知'}",
            f"标题: {metadata.get('header_path') or metadata.get('method_heading') or metadata.get('title') or '未知'}",
            f"方法: {metadata.get('method_heading') or metadata.get('method_name') or '未知'}",
            f"相似度: {score:.4f}",
            f"代码切片:\n{content}",
        ]
        return "\n".join(lines)

    def _parse_stack_frame(self, value: str) -> tuple[str, str, str]:
        """
        解析常见 Java 堆栈行，例如：
        cn.yq.modules.auth.service.impl.AuthServiceImpl.login(AuthServiceImpl.java:45)
        """
        match = re.search(r"([\w.$]+)\.(\w+)\((?:[\w.$]+\.java)?(?::(\d+))?\)", value)
        if not match:
            return "", "", ""
        return match.group(1), match.group(2), match.group(3) or ""
