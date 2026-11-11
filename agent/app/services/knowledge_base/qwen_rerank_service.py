import logging

import httpx
from llama_index.core.schema import NodeWithScore

from app.config.settings import get_settings

logger = logging.getLogger(__name__)


class QwenRerankService:
    """
    使用阿里云百炼的 qwen3-rerank 对 Milvus 召回结果做二次重排。

    适用场景：
    1. 先用向量检索快速召回一批候选 chunk；
    2. 再让 reranker 按“这个 chunk 是否真正回答了用户问题”重新排序；
    3. 只把最相关的前几条资料传给回答模型，减少噪声。
    """

    DEFAULT_INSTRUCT = "Given a web search query, retrieve relevant passages that answer the query."

    def __init__(self) -> None:
        self.settings = get_settings()

    def rerank(self, query: str, nodes: list[NodeWithScore], top_n: int | None = None) -> list[NodeWithScore]:
        """
        对候选知识节点重排。

        nodes 为空、只召回到 1 条、或未开启 rerank 时，直接返回原结果，避免无意义远程调用。
        """
        cleaned_query = query.strip()
        if not cleaned_query or len(nodes) <= 1 or not self.settings.qwen_rerank_enabled:
            return nodes

        api_key = (self.settings.qwen_rerank_api_key or self.settings.qwen_api_key).strip()
        if not api_key:
            logger.warning("未配置 QWEN_RERANK_API_KEY/QWEN_API_KEY，跳过 rerank")
            return nodes

        rerank_top_n = max(1, min(top_n or self.settings.qwen_rerank_top_n, len(nodes)))
        documents = [self._build_document(node) for node in nodes]
        payload = {
            "model": self.settings.qwen_rerank_model,
            "input": {
                "query": cleaned_query,
                "documents": documents,
            },
            "parameters": {
                "top_n": rerank_top_n,
                "return_documents": False,
            },
            # 当前使用的是百炼服务版 URL，请求体需要走 input / parameters 结构。
            # 当前是学生知识问答，更接近“问答检索”，不是 FAQ 相似问法匹配。
            "instruct": self.DEFAULT_INSTRUCT,
        }

        try:
            with httpx.Client(timeout=20.0) as client:
                response = client.post(
                    self.settings.qwen_rerank_url,
                    headers={
                        "Authorization": f"Bearer {api_key}",
                        "Content-Type": "application/json",
                    },
                    json=payload,
                )
                response.raise_for_status()
                body = response.json()
        except httpx.HTTPStatusError as ex:
            response_text = ex.response.text
            logger.exception(
                "Qwen rerank 调用失败，降级为原始向量排序: status=%s, body=%s",
                ex.response.status_code,
                response_text,
            )
            return nodes
        except Exception:
            logger.exception("Qwen rerank 调用失败，降级为原始向量排序")
            return nodes

        results = (((body or {}).get("output") or {}).get("results") or [])
        if not results:
            logger.warning("Qwen rerank 未返回 results，降级为原始向量排序: body=%s", body)
            return nodes

        reranked_nodes: list[NodeWithScore] = []
        used_indexes: set[int] = set()
        for item in results:
            if not isinstance(item, dict):
                continue
            index = item.get("index")
            if not isinstance(index, int) or index < 0 or index >= len(nodes):
                continue
            node = nodes[index]
            relevance_score = item.get("relevance_score")
            reranked_nodes.append(
                NodeWithScore(
                    node=node.node,
                    # 重排后的相关性分数更贴近“是否回答问题”，因此覆盖原始向量相似度。
                    score=float(relevance_score or 0),
                )
            )
            used_indexes.add(index)

        # 极端情况下，如果接口少返回了部分结果，补上剩余候选，避免资料被意外丢空。
        for index, node in enumerate(nodes):
            if index in used_indexes:
                continue
            reranked_nodes.append(node)

        return reranked_nodes[:rerank_top_n]

    def _build_document(self, node_with_score: NodeWithScore) -> str:
        """
        构造发给 reranker 的候选文档文本。

        这里把标题和章节路径拼在正文前面，让重排模型更容易理解这段 chunk 属于哪一节。
        单条文档仍然做长度截断，避免把过长 chunk 全量送过去增加 token 消耗。
        """
        node = node_with_score.node
        metadata = node.metadata or {}
        title = str(metadata.get("title") or "").strip()
        header_path = self._normalize_header_path(metadata.get("header_path"))
        content = node.get_content().strip()
        parts = [part for part in [title, header_path, content] if part]
        document = "\n".join(parts)
        # qwen3-rerank 单条 query/document 官方限制是 4000 token。
        # 这里先按字符做一层保守截断，避免超长 chunk 更容易触发 400。
        if len(document) > 3000:
            return document[:3000]
        return document

    def _normalize_header_path(self, value: object) -> str:
        if not isinstance(value, str):
            return ""
        return value.strip().strip(">").strip()
