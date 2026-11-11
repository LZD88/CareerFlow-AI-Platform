import json
import logging

from langchain_openai import ChatOpenAI

from app.config.settings import get_settings
from app.models.diagnose import DiagnoseLogRequest
from app.prompts.diagnose_prompt import DIAGNOSE_PROMPT_TEMPLATE

logger = logging.getLogger(__name__)


class LlmService:
    # 原始日志一般会很长，尤其是 Java 堆栈可能一条就几千字符。
    # 这里限制“最多发几条”和“每条最长多少字符”，是为了控制 prompt 体积：
    # 1. 避免模型上下文被无关日志塞满
    # 2. 避免一次请求 token 太多导致响应慢、成本高
    # 3. 让模型优先看 Java 侧已经结构化出来的关键信息
    MAX_RAW_LOG_COUNT = 5
    MAX_RAW_LOG_LENGTH = 1200

    def __init__(self) -> None:
        # settings 里包含方舟/OpenAI 兼容接口的 base_url、model、api_key 等配置。
        # 放到 service 初始化时读取，后面切模型或切供应商时不用动 DiagnoseService。
        self.settings = get_settings()

    def analyze(self, request: DiagnoseLogRequest, knowledge_contexts: list[str] | None = None) -> dict:
        """
        调用火山方舟的 OpenAI 兼容接口，返回结构化诊断结果。

        为什么这里单独封装一个 LLM Service：
        1. 后面如果模型从 Doubao 切到别的模型，只改这里。
        2. DiagnoseService 只关心“诊断流程”，不直接处理模型连接细节。
        """
        # 没有 API key 时主动抛错，让上层 DiagnoseService 进入 fallback。
        # 这样前端仍然能拿到模拟诊断结果，同时日志里也能明确看到是模型配置缺失。
        if not self.settings.ark_api_key:
            raise ValueError("ARK_API_KEY 未配置，无法调用火山方舟模型")

        logger.info(
            "准备调用火山方舟模型: base_url=%s, model=%s, guid=%s",
            self.settings.ark_base_url,
            self.settings.ark_model,
            request.guid,
        )

        llm = ChatOpenAI(
            api_key=self.settings.ark_api_key,
            base_url=self.settings.ark_base_url,
            model=self.settings.ark_model,
            # 日志诊断更看重稳定和可复现，不希望模型每次发散出完全不同的结论。
            # 所以 temperature 设低一点，让输出更接近“工程分析报告”。
            temperature=0.1,
        )

        # 模型最终看到的是两块上下文：
        # 1. context：Java 侧从火山日志中提取出来的结构化字段 + 压缩后的原始日志
        # 2. knowledge_context：RAG 从知识库召回的代码/文档片段
        #
        # 这相当于告诉模型：
        # - “事故现场”是什么：哪个 guid、哪个接口、什么异常、哪段堆栈
        # - “参考资料”是什么：相关代码、业务规则、历史说明
        context = self._build_context(request)
        knowledge_context = self._build_knowledge_context(knowledge_contexts or [])
        prompt = DIAGNOSE_PROMPT_TEMPLATE.format(context=context, knowledge_context=knowledge_context)
        logger.info(
            "发送给模型的诊断上下文摘要: guid=%s, raw_log_count=%s, context_length=%s, kb_context_count=%s",
            request.guid,
            min(len(request.raw_logs), self.MAX_RAW_LOG_COUNT),
            len(context),
            len(knowledge_contexts or []),
        )
        response = llm.invoke(prompt)

        # LangChain 返回的 content 大多数时候是 str。
        # 但为了兼容某些模型/网关返回 list、dict 等结构，这里统一转成字符串再解析。
        content = (
            response.content
            if isinstance(response.content, str)
            else json.dumps(response.content, ensure_ascii=False)
        )
        logger.info("火山方舟模型调用完成: guid=%s", request.guid)
        logger.info("模型原始返回: %s", content)
        return self._parse_json_response(content)

    def _build_context(self, request: DiagnoseLogRequest) -> str:
        """
        把结构化日志对象转换成模型容易阅读的纯文本。

        注意这里没有直接把 request.model_dump_json() 丢给模型，是因为：
        1. 纯文本字段名更稳定，模型更容易按 prompt 理解
        2. 空字段统一写成“未知”，可以减少模型误判字段不存在
        3. raw_logs 会先压缩，避免长堆栈占满上下文
        """
        raw_logs_text = self._compress_raw_logs(request.raw_logs)
        return (
            f"guid: {request.guid}\n"
            f"request_uri: {request.request_uri or '未知'}\n"
            f"http_method: {request.http_method or '未知'}\n"
            f"controller: {request.controller or '未知'}\n"
            f"request_args: {self._format_request_args(request.request_args)}\n"
            f"exception_class: {request.exception_class or '未知'}\n"
            f"exception_message: {request.exception_message or '未知'}\n"
            f"top_stack_frame: {request.top_stack_frame or '未知'}\n"
            f"raw_logs:\n{raw_logs_text}"
        )

    def _format_request_args(self, request_args: str | None) -> str:
        """
        控制请求参数进入 prompt 的长度。

        Java 侧已经按 ControllerLogAspect 做过脱敏和截断；这里再限制一次长度，
        避免大型请求体挤占异常堆栈和代码召回上下文。
        """
        if not request_args or not request_args.strip():
            return "未知"
        text = request_args.strip()
        if len(text) > 1500:
            return text[:1500] + "...(truncated)"
        return text

    def _compress_raw_logs(self, raw_logs: list[str]) -> str:
        """
        控制发给模型的日志规模，避免一次塞太多原始日志导致响应变慢。

        当前策略：
        1. 只取前 5 条关键日志
        2. 每条最长截断到 1200 个字符

        这里不做复杂摘要，而是保留原文片段：
        - 日志诊断需要异常类名、错误信息、堆栈方法这些“原始证据”
        - 如果提前用规则摘要，可能把真正关键的堆栈行删掉
        """
        if not raw_logs:
            return "无"

        compact_logs: list[str] = []
        for index, line in enumerate(raw_logs[: self.MAX_RAW_LOG_COUNT], start=1):
            text = line.strip()
            if len(text) > self.MAX_RAW_LOG_LENGTH:
                # 明确标记 truncated，避免模型误以为这就是完整日志。
                text = text[: self.MAX_RAW_LOG_LENGTH] + "...(truncated)"
            compact_logs.append(f"[log-{index}] {text}")
        return "\n".join(compact_logs)

    def _build_knowledge_context(self, knowledge_contexts: list[str]) -> str:
        """
        整理 RAG 召回内容。

        knowledge_contexts 通常来自 RagService，可能是代码切片、知识库文档、
        FAQ 或表格内容。这里最多取前 3 条，因为召回结果已经按相关性排序：
        - 太多知识库内容会稀释关键日志
        - 过长上下文也会让模型输出变慢
        """
        if not knowledge_contexts:
            return "无"
        lines: list[str] = []
        for index, item in enumerate(knowledge_contexts[:3], start=1):
            text = item.strip()
            if len(text) > 5000:
                # 单个知识库片段也要限长，否则一个大文档切片就可能占满 prompt。
                text = text[:5000] + "...(truncated)"
            lines.append(f"[kb-{index}] {text}")
        return "\n".join(lines)

    def _parse_json_response(self, content: str) -> dict:
        """
        模型不一定总是老老实实只回 JSON。
        这里做一层兜底，把最外层 JSON 片段截出来再解析，尽量提高容错。
        """
        text = content.strip()
        # 有些模型会在 JSON 前后加解释文字，比如：
        # “下面是诊断结果：{...}”
        # 所以这里截取第一个 { 到最后一个 } 之间的内容，再交给 json.loads。
        start = text.find("{")
        end = text.rfind("}")
        if start >= 0 and end > start:
            text = text[start:end + 1]
        data = json.loads(text)

        # 这里返回统一的 snake_case 字段，方便 Python 内部使用。
        # 同时兼容 camelCase，是因为 Java/前端常用 rootCause、evidenceLogs 这种命名。
        return {
            "summary": data.get("summary", ""),
            "possible_reason": data.get("possible_reason", ""),
            "root_cause": data.get("root_cause", "") or data.get("rootCause", ""),
            "confidence": self._parse_confidence(data.get("confidence")),
            "evidence_logs": self._parse_string_list(data.get("evidence_logs") or data.get("evidenceLogs")),
            "related_files": self._parse_string_list(data.get("related_files") or data.get("relatedFiles")),
            "next_actions": self._parse_string_list(data.get("next_actions") or data.get("nextActions")),
            "need_more_info": self._parse_bool(data.get("need_more_info") or data.get("needMoreInfo")),
            "missing_info": self._parse_string_list(data.get("missing_info") or data.get("missingInfo")),
            "manual_check_items": self._parse_string_list(
                data.get("manual_check_items") or data.get("manualCheckItems")
            ),
            "related_code": self._parse_string_list(data.get("related_code") or data.get("relatedCode")),
            "suggestions": self._parse_string_list(data.get("suggestions")),
        }

    def _parse_confidence(self, value: object) -> float:
        """
        把模型返回的置信度规范到 0~1。

        模型可能返回数字、字符串数字，甚至返回空值。
        对日志诊断来说，置信度宁可保守，不要因为解析失败给一个看似很高的值。
        """
        try:
            confidence = float(value)
        except (TypeError, ValueError):
            return 0.0
        return max(0.0, min(1.0, confidence))

    def _parse_string_list(self, value: object) -> list[str]:
        """
        把模型返回的列表字段统一成 list[str]。

        Prompt 要求模型返回数组，但实际模型偶尔会把单个建议返回成字符串。
        这里兼容这种情况，避免因为一个字段格式不稳导致整次诊断失败。
        """
        if isinstance(value, list):
            return [str(item) for item in value if item not in (None, "")]
        if isinstance(value, str) and value.strip():
            return [value.strip()]
        return []

    def _parse_bool(self, value: object) -> bool:
        """
        解析模型返回的布尔字段。

        不能直接用 bool(value)，因为 bool("false") 在 Python 里也是 True。
        所以这里显式判断常见的 true 写法，其它情况都按 False 处理。
        """
        if isinstance(value, bool):
            return value
        if isinstance(value, str):
            return value.strip().lower() in {"true", "1", "yes", "y"}
        return False
