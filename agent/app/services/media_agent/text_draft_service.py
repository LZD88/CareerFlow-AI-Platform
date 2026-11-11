import json
import logging

from langchain_openai import ChatOpenAI

from app.config.settings import get_settings
from app.models.media_agent import (
    DouyinImageTextDraft,
    DouyinVideoDraft,
    MediaTextDraftRequest,
    MediaTextDraftResponse,
    XiaohongshuNoteDraft,
)

logger = logging.getLogger(__name__)


class MediaTextDraftService:
    """自媒体 Agent 第二阶段：基于选题生成文字草稿。"""

    def __init__(self) -> None:
        self.settings = get_settings()

    def generate_text_draft(self, request: MediaTextDraftRequest) -> MediaTextDraftResponse:
        llm = self._build_llm()
        prompt = self._build_prompt(request)
        logger.info(
            "开始生成自媒体文字草稿: theme=%s, topic=%s, prompt_length=%s",
            request.theme,
            request.topic.topic,
            len(prompt),
        )

        response = llm.invoke(prompt)
        content = (
            response.content
            if isinstance(response.content, str)
            else json.dumps(response.content, ensure_ascii=False)
        )
        logger.info("自媒体文字草稿模型原始返回: %s", content)
        data = self._parse_json_response(content)
        return MediaTextDraftResponse(
            theme=request.theme,
            topic=request.topic,
            xiaohongshuNote=self._normalize_xiaohongshu(data.get("xiaohongshuNote") or data.get("xiaohongshu_note")),
            douyinVideo=self._normalize_douyin_video(data.get("douyinVideo") or data.get("douyin_video")),
            douyinImageText=self._normalize_douyin_image_text(
                data.get("douyinImageText") or data.get("douyin_image_text")
            ),
        )

    def _build_llm(self) -> ChatOpenAI:
        if self.settings.qwen_api_key:
            return ChatOpenAI(
                api_key=self.settings.qwen_api_key,
                base_url=self.settings.qwen_base_url,
                model=self.settings.qwen_model,
                temperature=0.65,
                extra_body={"enable_thinking": False},
            )
        if self.settings.ark_api_key:
            return ChatOpenAI(
                api_key=self.settings.ark_api_key,
                base_url=self.settings.ark_base_url,
                model=self.settings.ark_model,
                temperature=0.65,
            )
        raise ValueError("QWEN_API_KEY/ARK_API_KEY 未配置，无法调用大模型")

    def _build_prompt(self, request: MediaTextDraftRequest) -> str:
        topic = request.topic
        content_points = "\n".join(f"- {item}" for item in topic.contentPoints) or "无"
        platform_fit = "、".join(topic.platformFit) or "小红书、抖音"
        style = request.style or "专业但不生硬、自然可信"

        return f"""你是教育培训机构的自媒体文案 Agent。
请基于用户已经选中的“选题方向”，生成三种文字草稿：
1. 小红书图文版
2. 抖音短视频版
3. 抖音图文版

重要边界：
1. 只生成文字草稿，不生成图片，不输出图片 prompt。
2. 不要编造具体价格、校区、老师姓名、学生姓名、成绩数据、真实案例。
3. 不要使用“保证提分”“一定有效”“快速逆袭”等绝对化承诺。
4. 文案要自然可信，避免过度焦虑营销。
5. 标题/钩子需要给 3 到 5 个候选。
6. 只返回 JSON，不要输出 Markdown 或额外解释。

输出格式：
{{
  "xiaohongshuNote": {{
    "titles": [],
    "coverText": "",
    "body": "",
    "imagePagePlan": [],
    "hashtags": []
  }},
  "douyinVideo": {{
    "hooks": [],
    "script": "",
    "shotList": [],
    "subtitles": [],
    "hashtags": []
  }},
  "douyinImageText": {{
    "titles": [],
    "coverText": "",
    "body": "",
    "imagePagePlan": [],
    "hashtags": []
  }}
}}

写作要求：
- 小红书图文：正文适合直接发布，段落清晰，有共鸣、有解释、有轻行动建议。
- 抖音短视频：script 适合真人口播，时长约 45-75 秒，shotList 给 4-6 个镜头建议。
- 抖音图文：正文比小红书更短、更直给，imagePagePlan 给 4-6 页图文页结构。
- hashtags 每个平台 5-8 个，保持中文标签，不要带 # 号。

用户主题：
{request.theme}

选题方向：
{topic.topic}

内容角度：
{topic.angle or "未指定"}

目标人群：
{topic.audience or "未指定"}

推荐理由：
{topic.reason or "无"}

内容要点：
{content_points}

适配平台：
{platform_fit}

内容风格：
{style}
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

    def _normalize_xiaohongshu(self, value: object) -> XiaohongshuNoteDraft:
        data = value if isinstance(value, dict) else {}
        return XiaohongshuNoteDraft(
            titles=self._parse_string_list(data.get("titles"))[:5],
            coverText=str(data.get("coverText") or data.get("cover_text") or "").strip(),
            body=str(data.get("body") or "").strip(),
            imagePagePlan=self._parse_string_list(data.get("imagePagePlan") or data.get("image_page_plan"))[:8],
            hashtags=self._parse_string_list(data.get("hashtags"))[:10],
        )

    def _normalize_douyin_video(self, value: object) -> DouyinVideoDraft:
        data = value if isinstance(value, dict) else {}
        return DouyinVideoDraft(
            hooks=self._parse_string_list(data.get("hooks"))[:5],
            script=str(data.get("script") or "").strip(),
            shotList=self._parse_string_list(data.get("shotList") or data.get("shot_list"))[:8],
            subtitles=self._parse_string_list(data.get("subtitles"))[:10],
            hashtags=self._parse_string_list(data.get("hashtags"))[:10],
        )

    def _normalize_douyin_image_text(self, value: object) -> DouyinImageTextDraft:
        data = value if isinstance(value, dict) else {}
        return DouyinImageTextDraft(
            titles=self._parse_string_list(data.get("titles"))[:5],
            coverText=str(data.get("coverText") or data.get("cover_text") or "").strip(),
            body=str(data.get("body") or "").strip(),
            imagePagePlan=self._parse_string_list(data.get("imagePagePlan") or data.get("image_page_plan"))[:8],
            hashtags=self._parse_string_list(data.get("hashtags"))[:10],
        )

    def _parse_string_list(self, value: object) -> list[str]:
        if isinstance(value, list):
            return [str(item).strip() for item in value if str(item).strip()]
        if isinstance(value, str) and value.strip():
            return [value.strip()]
        return []
