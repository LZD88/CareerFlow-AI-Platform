import json
import logging

from langchain_openai import ChatOpenAI

from app.config.settings import get_settings
from app.models.media_agent import MediaTopicItem, MediaTopicRequest, MediaTopicResponse

logger = logging.getLogger(__name__)


class MediaTopicService:
    """自媒体 Agent 第一阶段：根据主题生成选题方向。"""

    def __init__(self) -> None:
        self.settings = get_settings()

    def generate_topics(self, request: MediaTopicRequest) -> MediaTopicResponse:
        llm = self._build_llm()
        prompt = self._build_prompt(request)
        logger.info(
            "开始生成自媒体选题: theme=%s, platforms=%s, topic_count=%s, prompt_length=%s",
            request.theme,
            request.platforms,
            request.topicCount,
            len(prompt),
        )

        response = llm.invoke(prompt)
        content = (
            response.content
            if isinstance(response.content, str)
            else json.dumps(response.content, ensure_ascii=False)
        )
        logger.info("自媒体选题模型原始返回: %s", content)
        data = self._parse_json_response(content)
        topics = self._normalize_topics(data, request)
        if not topics:
            raise ValueError("模型未返回有效选题")
        return MediaTopicResponse(theme=request.theme, topics=topics)

    def _build_llm(self) -> ChatOpenAI:
        if self.settings.qwen_api_key:
            return ChatOpenAI(
                api_key=self.settings.qwen_api_key,
                base_url=self.settings.qwen_base_url,
                model=self.settings.qwen_router_model or self.settings.qwen_model,
                temperature=0.6,
                extra_body={"enable_thinking": False},
            )
        if self.settings.ark_api_key:
            return ChatOpenAI(
                api_key=self.settings.ark_api_key,
                base_url=self.settings.ark_base_url,
                model=self.settings.ark_model,
                temperature=0.6,
            )
        raise ValueError("QWEN_API_KEY/ARK_API_KEY 未配置，无法调用大模型")

    def _build_prompt(self, request: MediaTopicRequest) -> str:
        platforms = "、".join(request.platforms) if request.platforms else "小红书、抖音"
        audience = request.audience or "未指定，请根据主题合理判断"
        goal = request.goal or "未指定，请兼顾品牌信任和内容传播"
        style = request.style or "专业、清晰、自然"

        return f"""你是自媒体内容策划 Agent，负责为运营人员生成“选题方向”。

重要边界：
1. 你生成的是选题方向，不是最终标题。
2. 选题要回答“这篇内容要讲什么”，不要写成标题党、广告语或抖音开场白。
3. 每个选题都要能继续扩展成小红书图文或抖音口播内容。
4. 不要编造具体价格、校区、学生案例、成绩数据。
5. 教育培训类内容避免绝对化承诺，例如“保证提分”“一定有效”“快速逆袭”。
6. 只返回 JSON，不要输出 Markdown 或额外解释。

输出格式：
{{
  "topics": [
    {{
      "topic": "选题方向，表达内容要讲什么",
      "angle": "内容角度",
      "audience": "该选题面向的具体用户",
      "reason": "推荐该选题的原因",
      "contentPoints": ["内容要点 1", "内容要点 2", "内容要点 3"],
      "platformFit": ["xiaohongshu", "douyin"],
      "score": 85
    }}
  ]
}}

生成要求：
- 返回 {request.topicCount} 个选题。
- score 是 0 到 100 的整数，越高表示越推荐。
- contentPoints 每个选题返回 3 到 5 条。
- platformFit 只能从用户目标平台中选择。
- topic 字段要像“内容方向”，不要像“标题”。

用户需求：
主题：{request.theme}
目标平台：{platforms}
目标人群：{audience}
内容目标：{goal}
内容风格：{style}
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

    def _normalize_topics(self, data: dict, request: MediaTopicRequest) -> list[MediaTopicItem]:
        raw_topics = data.get("topics") or data.get("topicList") or []
        if not isinstance(raw_topics, list):
            return []

        target_platforms = request.platforms or ["xiaohongshu", "douyin"]
        normalized: list[MediaTopicItem] = []
        seen_topics: set[str] = set()
        for item in raw_topics:
            if not isinstance(item, dict):
                continue
            topic = str(item.get("topic") or "").strip()
            if not topic or topic in seen_topics:
                continue
            seen_topics.add(topic)
            normalized.append(
                MediaTopicItem(
                    topic=topic,
                    angle=str(item.get("angle") or "").strip(),
                    audience=str(item.get("audience") or request.audience or "").strip(),
                    reason=str(item.get("reason") or "").strip(),
                    contentPoints=self._parse_string_list(item.get("contentPoints") or item.get("content_points"))[:5],
                    platformFit=self._normalize_platforms(item.get("platformFit") or item.get("platform_fit"), target_platforms),
                    score=self._parse_score(item.get("score")),
                )
            )
            if len(normalized) >= request.topicCount:
                break
        return normalized

    def _normalize_platforms(self, value: object, target_platforms: list[str]) -> list[str]:
        platforms = self._parse_string_list(value)
        allowed = set(target_platforms)
        result = [platform for platform in platforms if platform in allowed]
        return result or target_platforms

    def _parse_string_list(self, value: object) -> list[str]:
        if isinstance(value, list):
            return [str(item).strip() for item in value if str(item).strip()]
        if isinstance(value, str) and value.strip():
            return [value.strip()]
        return []

    def _parse_score(self, value: object) -> int:
        try:
            score = int(float(value))
        except (TypeError, ValueError):
            return 0
        return max(0, min(100, score))
