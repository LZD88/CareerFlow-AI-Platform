import json
import logging

import httpx

from app.config.settings import get_settings
from app.models.diagnose import DiagnoseLogRequest

logger = logging.getLogger(__name__)


class VolcengineKnowledgeSearchService:
    """
    火山引擎知识库检索实现。

    这份实现暂时保留，方便回滚或对比 Milvus 检索效果；日志诊断主流程默认不再使用它。
    """

    def __init__(self) -> None:
        self.settings = get_settings()

    def retrieve(self, request: DiagnoseLogRequest) -> list[str]:
        if not self.settings.kb_api_key or not self.settings.kb_service_resource_id:
            logger.info("火山知识库配置不完整，跳过 RAG 检索")
            return []

        if not self.settings.kb_service_resource_id.startswith("kb-service-"):
            logger.error(
                "火山知识库 service_resource_id 格式不正确: guid=%s, service_resource_id=%s。"
                "当前接口要求传知识库服务 ID，通常形如 kb-service-xxx，而不是知识库 ID。",
                request.guid,
                self.settings.kb_service_resource_id,
            )
            return []

        query = self._build_query(request)
        payload = {
            "service_resource_id": self.settings.kb_service_resource_id,
            "messages": [
                {
                    "role": "user",
                    "content": query,
                }
            ],
            "stream": False,
        }

        url = self._build_url()
        headers = {
            "Accept": "application/json",
            "Content-Type": "application/json;charset=UTF-8",
            "Authorization": f"Bearer {self.settings.kb_api_key}",
        }
        logger.info(
            "开始调用火山知识库服务: url=%s, guid=%s, service_resource_id=%s",
            url,
            request.guid,
            self.settings.kb_service_resource_id,
        )

        try:
            with httpx.Client(timeout=20.0) as client:
                response = client.post(url, headers=headers, json=payload)
                if response.status_code >= 400:
                    logger.error(
                        "火山知识库服务返回异常: guid=%s, status=%s, body=%s",
                        request.guid,
                        response.status_code,
                        response.text,
                    )
                response.raise_for_status()
                data = response.json()
        except Exception:
            logger.exception("火山知识库检索失败: guid=%s", request.guid)
            return []

        contexts = self._extract_contexts(data)
        logger.info("火山知识库检索完成: guid=%s, context_count=%s", request.guid, len(contexts))
        return contexts

    def _build_url(self) -> str:
        base_url = self.settings.kb_base_url.rstrip("/")
        return f"{base_url}/api/knowledge/service/chat"

    def _build_query(self, request: DiagnoseLogRequest) -> str:
        parts = [
            "请检索与以下 Java 异常日志最相关的代码、类、方法和异常处理逻辑。",
            f"请求路径: {request.request_uri or '未知'}",
            f"控制器: {request.controller or '未知'}",
            f"请求参数: {request.request_args or '未知'}",
            f"异常类型: {request.exception_class or '未知'}",
            f"异常信息: {request.exception_message or '未知'}",
            f"首个堆栈: {request.top_stack_frame or '未知'}",
        ]
        return "\n".join(parts)

    def _extract_contexts(self, response_json: dict) -> list[str]:
        """
        按火山 knowledge service chat 返回结构提取上下文。
        """
        candidates: list[str] = []

        def to_text(value: object) -> str:
            if isinstance(value, str) and value.strip():
                return value.strip()
            if isinstance(value, (int, float)):
                return str(value)
            return ""

        def append_line(lines: list[str], label: str, value: object) -> None:
            text = to_text(value)
            if text:
                lines.append(f"{label}: {text}")

        def rank_key(item: dict) -> tuple[int, float, float]:
            rerank_position = item.get("rerank_position")
            if isinstance(rerank_position, int):
                return rerank_position, 0.0, 0.0
            return 999999, -float(item.get("rerank_score") or 0), -float(item.get("score") or 0)

        def append_retrieve_item(item: dict) -> None:
            lines: list[str] = []
            append_line(lines, "切片类型", item.get("chunk_type"))
            append_line(lines, "切片标题", item.get("chunk_title"))
            append_line(lines, "FAQ原始问题", item.get("original_question"))

            doc_info = item.get("doc_info") or {}
            append_line(lines, "来源", doc_info.get("doc_name") or doc_info.get("title"))

            content = item.get("md_content") if item.get("chunk_type") in {"table", "mixed-table"} else None
            if not to_text(content):
                content = item.get("content")
            content_text = to_text(content)
            if content_text:
                lines.append(f"切片内容:\n{content_text}")

            field_lines = [
                f"{field['field_name'].strip()}: {field['field_value']}"
                for field in item.get("table_chunk_fields") or []
                if to_text(field.get("field_name")) and field.get("field_value") not in (None, "")
            ]
            if field_lines:
                lines.append("结构化字段:\n" + "\n".join(field_lines))

            if lines:
                candidates.append("\n".join(lines))

        data = response_json.get("data") or {}
        ranked_items = sorted(data.get("result_list") or [], key=rank_key)
        for item in ranked_items[:5]:
            append_retrieve_item(item)

        if candidates:
            return candidates[:5]

        raw = json.dumps(response_json, ensure_ascii=False)
        return [raw[:2000] + ("...(truncated)" if len(raw) > 2000 else "")]
