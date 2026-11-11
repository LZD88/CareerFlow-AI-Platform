from app.models.student_chat import QuestionContext, StudentChatStreamRequest
from app.services.student_chat.rag_router_service import RagRouterService


def build_request(question: str) -> StudentChatStreamRequest:
    request = StudentChatStreamRequest()
    request.question = QuestionContext(content=question)
    return request


def test_router_regex_routes_internal_question_to_rag() -> None:
    decision = RagRouterService()._decide_by_regex("学费可以退吗")

    assert decision is not None
    assert decision.need_rag is True
    assert decision.source == "regex"


def test_router_regex_routes_short_general_question_without_rag() -> None:
    decision = RagRouterService()._decide_by_regex("什么是 HashMap")

    assert decision is not None
    assert decision.need_rag is False
    assert decision.source == "regex"


def test_router_regex_defers_uncertain_question_to_llm() -> None:
    decision = RagRouterService()._decide_by_regex("事务隔离级别和传播机制有什么区别")

    assert decision is None


def test_router_parses_json_wrapped_in_markdown() -> None:
    payload = RagRouterService()._parse_json_response(
        '```json\n{"need_rag": true, "confidence": 0.91, "reason": "涉及内部课程资料"}\n```'
    )

    assert payload["need_rag"] is True
    assert payload["confidence"] == 0.91


def test_router_disables_thinking_for_small_classification_budget(monkeypatch) -> None:
    from types import SimpleNamespace
    import app.services.student_chat.rag_router_service as router_module

    options = {}

    class FakeChat:
        def __init__(self, **kwargs):
            options.update(kwargs)

        def invoke(self, messages):
            return SimpleNamespace(content='{"need_rag": false, "confidence": 0.98, "reason": "通用编程问题"}')

    monkeypatch.setattr(router_module, "ChatOpenAI", FakeChat)
    router = RagRouterService()
    router.settings = router.settings.model_copy(update={"qwen_api_key": "test-only"})
    decision = router.decide(build_request("事务隔离级别和传播机制有什么区别"))

    assert decision.need_rag is False
    assert decision.source == "llm"
    assert options["extra_body"]["enable_thinking"] is False
    assert options["timeout"] == 15
