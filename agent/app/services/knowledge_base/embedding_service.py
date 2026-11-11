import logging
from json import JSONDecodeError

import httpx

from app.config.settings import get_settings

logger = logging.getLogger(__name__)


class EmbeddingService:
    """
    文本向量化服务。

    当前按火山方舟「向量化 API」直接发 HTTP 请求：
    - base_url: https://ark.cn-beijing.volces.com/api/v3
    - path: /embeddings/multimodal
    - model: doubao-embedding-vision-251215 或控制台提供的模型/endpoint 名

    这里没有继续用 OpenAI SDK 的 embeddings.create，是因为 doubao-embedding-vision
    属于多模态向量模型，文本输入也需要按多模态 input item 结构提交：
    [{"type": "text", "text": "..."}]。
    """

    def __init__(self) -> None:
        self.settings = get_settings()

    def embed_text(self, text: str) -> list[float]:
        """
        将单段文本转换成稠密向量。

        后续文档入库和学生提问检索都必须经过这里生成向量，保证文档向量和问题向量
        使用同一个模型、同一个维度。
        """
        normalized_text = text.strip()
        if not normalized_text:
            raise ValueError("向量化文本不能为空")

        logger.info(
            "开始调用 Embedding 模型: model=%s, dimension=%s, url=%s, text_length=%s",
            self.settings.embedding_model,
            self.settings.embedding_dimensions,
            self._embedding_url(),
            len(normalized_text),
        )

        response_json = self._post_embedding(normalized_text)
        embedding = self._extract_embedding(response_json)
        self._validate_dimension(embedding)
        return embedding

    def health(self) -> dict:
        """
        调用一次真实 embedding，用来确认 API Key、Base URL、模型名和维度都正确。
        """
        embedding = self.embed_text("Milvus 向量数据库连接测试")
        return {
            "ok": True,
            "model": self.settings.embedding_model,
            "base_url": self.settings.embedding_base_url,
            "path": self.settings.embedding_path,
            "dimension": len(embedding),
            "message": "Embedding 模型调用成功",
        }

    def _post_embedding(self, text: str) -> dict:
        api_key = self.settings.embedding_api_key or self.settings.ark_api_key
        if not api_key:
            raise ValueError("EMBEDDING_API_KEY 或 ARK_API_KEY 未配置，无法调用 Embedding 模型")

        payload = self._build_payload(text)
        url = self._embedding_url()
        headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        }

        try:
            with httpx.Client(timeout=30.0) as client:
                response = client.post(url, headers=headers, json=payload)
                response.raise_for_status()
                return response.json()
        except httpx.HTTPStatusError as ex:
            logger.exception(
                "Embedding HTTP 状态异常: status=%s, url=%s, model=%s, response=%s, payload=%s",
                ex.response.status_code,
                url,
                self.settings.embedding_model,
                ex.response.text[:2000],
                self._safe_payload_for_log(payload),
            )
            raise ValueError(self._build_http_error_message(ex)) from ex
        except httpx.RequestError as ex:
            logger.exception(
                "Embedding HTTP 请求异常: url=%s, model=%s, payload=%s",
                url,
                self.settings.embedding_model,
                self._safe_payload_for_log(payload),
            )
            raise ValueError(f"Embedding 请求失败，无法连接向量化服务: {ex}") from ex
        except JSONDecodeError as ex:
            logger.exception(
                "Embedding 响应不是合法 JSON: url=%s, model=%s",
                url,
                self.settings.embedding_model,
            )
            raise ValueError("Embedding 响应不是合法 JSON，请查看 Python 日志中的原始异常") from ex
        except Exception as ex:
            logger.exception(
                "Embedding 调用发生未知异常: url=%s, model=%s, payload=%s",
                url,
                self.settings.embedding_model,
                self._safe_payload_for_log(payload),
            )
            raise ValueError(f"Embedding 调用发生未知异常: {ex}") from ex

    def _build_payload(self, text: str) -> dict:
        payload = {
            "model": self.settings.embedding_model,
            "input": [
                {
                    "type": "text",
                    "text": text,
                }
            ],
            "encoding_format": "float",
        }
        if self.settings.embedding_dimensions > 0:
            payload["dimensions"] = self.settings.embedding_dimensions
        return payload

    def _embedding_url(self) -> str:
        base_url = self.settings.embedding_base_url.rstrip("/")
        if base_url.endswith("/embeddings") or base_url.endswith("/embeddings/multimodal"):
            return base_url

        path = self.settings.embedding_path or "/embeddings/multimodal"
        if not path.startswith("/"):
            path = f"/{path}"
        return f"{base_url}{path}"

    def _extract_embedding(self, response_json: dict) -> list[float]:
        data = self._extract_response_data(response_json)
        if not data:
            raise ValueError(f"Embedding 模型未返回向量数据: {response_json}")

        first_item = data[0] if isinstance(data, list) else data
        logger.info(
            "Embedding 响应向量数据结构: data_type=%s, item_type=%s, item_keys=%s",
            type(data).__name__,
            type(first_item).__name__,
            list(first_item.keys()) if isinstance(first_item, dict) else None,
        )

        embedding = self._extract_embedding_from_item(first_item)
        if not isinstance(embedding, list):
            raise ValueError(f"Embedding 响应格式不符合预期: {response_json}")
        return embedding

    def _extract_response_data(self, response_json: dict) -> object:
        """
        兼容不同顶层响应结构。

        常见结构：
        - {"data": [{"embedding": [...]}]}
        - {"data": {"embedding": {"dense": [...]}}}
        - {"result": {"data": [{"dense": [...] }]}}
        - {"result": {"data": {"dense": [...] }}}
        """
        data = response_json.get("data")
        if data:
            return data

        result = response_json.get("result")
        if isinstance(result, dict):
            return result.get("data")
        return None

    def _extract_embedding_from_item(self, item: object) -> list[float] | None:
        """
        兼容不同火山向量化接口返回格式。

        常见返回可能是：
        1. OpenAI-compatible: {"embedding": [...]}
        2. 多模态向量化: {"embedding": {"dense": [...]}}
        3. VikingDB 风格: {"dense": [...]}
        4. 少数 SDK/网关可能直接返回 [...]
        """
        if isinstance(item, list):
            return item
        if not isinstance(item, dict):
            return None

        embedding = item.get("embedding")
        if isinstance(embedding, list):
            return embedding
        if isinstance(embedding, dict):
            dense = embedding.get("dense")
            if isinstance(dense, list):
                return dense

        dense = item.get("dense")
        if isinstance(dense, list):
            return dense

        vector = item.get("vector")
        if isinstance(vector, list):
            return vector

        return None

    def _validate_dimension(self, embedding: list[float]) -> None:
        actual_dimension = len(embedding)
        expected_dimension = self.settings.embedding_dimensions
        if expected_dimension <= 0:
            return
        if actual_dimension != expected_dimension:
            raise ValueError(
                f"Embedding 返回维度不匹配: expected={expected_dimension}, actual={actual_dimension}。"
                "请确认 EMBEDDING_DIMENSIONS 和 Milvus collection 维度一致。"
            )

    def _build_http_error_message(self, ex: httpx.HTTPStatusError) -> str:
        raw = ex.response.text
        if "InvalidSubscription" in raw:
            return (
                "Embedding 模型调用失败：当前账号未开通对应向量化能力，或订阅已过期。"
                "请确认方舟控制台中该向量化模型/API Key 已开通。"
            )
        if "dimensions" in raw.lower():
            return (
                "Embedding 模型不支持当前 EMBEDDING_DIMENSIONS 参数。"
                "请确认模型支持当前维度，或重建 Milvus collection 以匹配模型实际维度。"
            )
        return (
            f"Embedding 模型请求失败: status={ex.response.status_code}, body={raw[:1000]}。"
            "请检查模型名、Base URL、path、API Key 和 input 格式。"
        )

    def _safe_payload_for_log(self, payload: dict) -> dict:
        """
        打日志时保留请求结构，但截断文本，避免长文档或学生内容把日志刷爆。
        """
        safe_payload = dict(payload)
        input_items = safe_payload.get("input")
        if isinstance(input_items, list):
            safe_items = []
            for item in input_items:
                if isinstance(item, dict):
                    safe_item = dict(item)
                    text = safe_item.get("text")
                    if isinstance(text, str) and len(text) > 200:
                        safe_item["text"] = text[:200] + "...(truncated)"
                    safe_items.append(safe_item)
                else:
                    safe_items.append(item)
            safe_payload["input"] = safe_items
        return safe_payload
