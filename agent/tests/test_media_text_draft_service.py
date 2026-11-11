from app.models.media_agent import MediaTextDraftRequest, MediaTopicItem
from app.services.media_agent.text_draft_service import MediaTextDraftService


def build_request() -> MediaTextDraftRequest:
    return MediaTextDraftRequest(
        theme="少儿编程春季招生",
        topic=MediaTopicItem(
            topic="家长对少儿编程的常见误解",
            angle="纠偏科普",
            audience="6-12 岁孩子家长",
            reason="适合降低家长排斥感",
            contentPoints=["少儿编程不是提前学成人代码", "重点是逻辑和问题拆解"],
            platformFit=["xiaohongshu", "douyin"],
            score=88,
        ),
        style="专业但不生硬",
    )


def test_build_prompt_generates_text_only() -> None:
    prompt = MediaTextDraftService()._build_prompt(build_request())

    assert "只生成文字草稿，不生成图片，不输出图片 prompt" in prompt
    assert "小红书图文版" in prompt
    assert "抖音短视频版" in prompt
    assert "抖音图文版" in prompt


def test_normalize_text_draft_blocks() -> None:
    service = MediaTextDraftService()
    response = service.generate_text_draft.__annotations__

    assert response["return"].__name__ == "MediaTextDraftResponse"


def test_normalize_xiaohongshu_limits_titles() -> None:
    draft = MediaTextDraftService()._normalize_xiaohongshu(
        {
            "titles": ["标题1", "标题2", "标题3", "标题4", "标题5", "标题6"],
            "coverText": "封面",
            "body": "正文",
            "imagePagePlan": ["第1页", "第2页"],
            "hashtags": ["少儿编程"],
        }
    )

    assert draft.titles == ["标题1", "标题2", "标题3", "标题4", "标题5"]
    assert draft.coverText == "封面"
    assert draft.imagePagePlan == ["第1页", "第2页"]
