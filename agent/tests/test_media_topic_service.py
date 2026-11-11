from app.models.media_agent import MediaTopicRequest
from app.services.media_agent.topic_service import MediaTopicService


def test_build_prompt_marks_topic_as_not_title() -> None:
    service = MediaTopicService()
    prompt = service._build_prompt(
        MediaTopicRequest(
            theme="少儿编程春季招生",
            platforms=["xiaohongshu", "douyin"],
            audience="6-12 岁孩子家长",
            goal="招生转化",
            style="专业但不生硬",
            topicCount=6,
        )
    )

    assert "你生成的是选题方向，不是最终标题" in prompt
    assert "不要写成标题党、广告语或抖音开场白" in prompt
    assert "返回 6 个选题" in prompt


def test_parse_json_response_extracts_json_from_extra_text() -> None:
    service = MediaTopicService()
    data = service._parse_json_response('下面是结果：{"topics": [{"topic": "家长常见误解"}]}')

    assert data["topics"][0]["topic"] == "家长常见误解"


def test_normalize_topics_clamps_score_and_filters_platforms() -> None:
    service = MediaTopicService()
    request = MediaTopicRequest(
        theme="少儿编程",
        platforms=["xiaohongshu", "douyin"],
        topicCount=3,
    )

    topics = service._normalize_topics(
        {
            "topics": [
                {
                    "topic": "家长对少儿编程的常见误解",
                    "angle": "纠偏科普",
                    "contentPoints": ["不是提前学成人代码", "重点是逻辑训练", "不同年龄目标不同"],
                    "platformFit": ["xiaohongshu", "unknown"],
                    "score": 120,
                },
                {
                    "topic": "家长对少儿编程的常见误解",
                    "score": 80,
                },
                {
                    "topic": "低龄孩子学编程的合适目标",
                    "platformFit": [],
                    "score": "82",
                },
            ]
        },
        request,
    )

    assert len(topics) == 2
    assert topics[0].score == 100
    assert topics[0].platformFit == ["xiaohongshu"]
    assert topics[1].score == 82
    assert topics[1].platformFit == ["xiaohongshu", "douyin"]
