import json
import logging
import re
from dataclasses import dataclass

from langchain_core.messages import HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI

from app.config.settings import get_settings
from app.models.student_chat import StudentChatStreamRequest

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class RagRouteDecision:
    need_rag: bool
    source: str
    reason: str
    confidence: float = 1.0


class RagRouterService:
    """
    判断学生问题是否需要查询向量知识库。

    策略：
    1. 先用本地正则判断“明显属于内部/课程/机构问题”的场景，命中后直接走 RAG；
    2. 正则没命中，再调用 qwen3.6-flash 做语义分类；
    3. 模型判断失败或置信度不足时，默认走 RAG，避免漏查内部知识。

    这里的正则不是为了覆盖所有业务词，而是为了拦截最明显的问题，减少一次小模型调用。
    真正解决“学费、合同、退费、未来新增业务词”等开放问题的是 qwen3.6-flash 的语义判断。
    """

    # 这些模式只做“强命中直接 RAG”。
    # 未来新增业务词没写在这里也没关系，没命中会继续交给 qwen3.6-flash 判断。
    RAG_HINT_PATTERN = re.compile(
        r"("
        r"我们|咱们|你们|本校|学校|机构|CareerFlow|平台|系统|账号|登录|报名|合同|协议|"
        r"学费|费用|缴费|付款|退费|退款|发票|收据|"
        r"课程|班级|就业班|班主任|老师|助教|讲师|作业|课表|上课|下课|请假|补课|"
        r"资料|讲义|课件|项目|实训|考试|证书|就业|简历|面试|"
        r"第[一二三四五六七八九十0-9]+天|第[一二三四五六七八九十0-9]+节|今天|明天|昨天"
        r")"
    )

    GENERAL_PATTERN = re.compile(
        r"^("
        r"你好|您好|谢谢|感谢|再见|"
        r"什么是|解释一下|介绍一下|帮我写|写一段|生成一段|翻译|润色|总结一下"
        r")"
    )

    def __init__(self) -> None:
        self.settings = get_settings()

    def decide(self, request: StudentChatStreamRequest) -> RagRouteDecision:
        question = request.question.content.strip()
        if not question:
            return RagRouteDecision(False, "empty", "空问题不检索", 1.0)

        regex_decision = self._decide_by_regex(question)
        if regex_decision is not None:
            return regex_decision

        return self._decide_by_llm(request)

    def _decide_by_regex(self, question: str) -> RagRouteDecision | None:
        if self.RAG_HINT_PATTERN.search(question):
            return RagRouteDecision(True, "regex", "命中内部知识库相关表达", 1.0)

        # 这里只拦截很短、很明确的通用问题或闲聊。
        # 稍微复杂一点的通用问题仍交给 qwen3.6-flash 判断，避免误判。
        if len(question) <= 30 and self.GENERAL_PATTERN.search(question):
            return RagRouteDecision(False, "regex", "命中明显通用/闲聊表达", 0.95)

        return None

    def _decide_by_llm(self, request: StudentChatStreamRequest) -> RagRouteDecision:
        if not self.settings.qwen_api_key:
            logger.warning("QWEN_API_KEY 未配置，RAG Router 默认走 RAG")
            return RagRouteDecision(True, "fallback", "router 模型未配置，默认检索", 0.5)

        try:
            llm = ChatOpenAI(
                api_key=self.settings.qwen_api_key,
                base_url=self.settings.qwen_base_url,
                model=self.settings.qwen_router_model,
                temperature=0,
                max_tokens=160,
                streaming=False,
                extra_body={"enable_thinking": False},
                timeout=15,
                max_retries=0,
            )
            response = llm.invoke(self._build_router_messages(request))
            payload = self._parse_json_response(str(response.content))
            need_rag = bool(payload.get("need_rag", True))
            confidence = float(payload.get("confidence", 0.5) or 0.5)
            reason = str(payload.get("reason") or "qwen router 判断")

            # 不确定时默认走 RAG，保证内部问题不被漏查。
            if confidence < 0.65:
                return RagRouteDecision(True, "llm_low_confidence", f"低置信度默认检索：{reason}", confidence)

            return RagRouteDecision(need_rag, "llm", reason, confidence)
        except Exception as ex:
            logger.exception("RAG Router 调用 qwen3.6-flash 失败，默认走 RAG")
            return RagRouteDecision(True, "fallback", f"router 调用失败，默认检索：{ex}", 0.5)

    def _build_router_messages(self, request: StudentChatStreamRequest) -> list:
        student = request.student
        context = (
            f"学生课程: {student.courseName or '-'}\n"
            f"学生班级: {student.className or '-'}\n"
            f"当前问题: {request.question.content.strip()}"
        )
        return [
            SystemMessage(
                content=(
                    "你是学生问答系统的 RAG 路由器，只判断是否需要查询内部知识库。\n"
                    "需要查询知识库的情况：\n"
                    "- 问题依赖机构内部规则、课程安排、班级信息、老师要求、作业要求、学费政策、退费规则、"
                    "就业服务、平台使用说明、内部资料。\n"
                    "- 问题中出现上下文依赖，例如我们这里、你们、课程里、老师说、平台上、我的班级、我报名的。\n"
                    "- 只靠通用常识无法准确回答。\n"
                    "不需要查询知识库的情况：\n"
                    "- 通用知识解释、通用编程问题、通用代码生成、闲聊、翻译、润色。\n"
                    "只返回 JSON，不要输出其他文字："
                    '{"need_rag": true/false, "confidence": 0到1之间的小数, "reason": "简短原因"}'
                )
            ),
            HumanMessage(content=context),
        ]

    def _parse_json_response(self, content: str) -> dict:
        text = content.strip()
        if text.startswith("```"):
            text = re.sub(r"^```(?:json)?", "", text).strip()
            text = re.sub(r"```$", "", text).strip()

        match = re.search(r"\{.*\}", text, flags=re.DOTALL)
        if match:
            text = match.group(0)

        payload = json.loads(text)
        if not isinstance(payload, dict):
            raise ValueError(f"router 返回不是 JSON object: {content}")
        return payload
