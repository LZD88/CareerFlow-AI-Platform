import logging

from app.models.diagnose import DiagnoseLogRequest, DiagnoseLogResponse
from app.services.log_diagnosis.llm_service import LlmService
from app.services.log_diagnosis.rag_service import RagService

logger = logging.getLogger(__name__)


class DiagnoseService:
    def __init__(self) -> None:
        self.llm_service = LlmService()
        self.rag_service = RagService()

    def diagnose(self, request: DiagnoseLogRequest) -> DiagnoseLogResponse:
        """
        这是当前日志诊断的主入口。

        当前流程：
        1. 先尝试调用火山方舟模型做真实分析
        2. 如果模型调用失败，再回退到本地 mock 逻辑

        这样做的好处是：
        - 联调阶段更稳，不会因为模型临时异常导致整个接口不可用
        - 你前端仍然能拿到一份可展示的结果
        """
        try:
            logger.info(
                "开始日志诊断: guid=%s, request_uri=%s, exception_class=%s, exception_message=%s",
                request.guid,
                request.request_uri,
                request.exception_class,
                request.exception_message,
            )
            knowledge_contexts = self.rag_service.retrieve(request)
            llm_result = self.llm_service.analyze(request, knowledge_contexts)
            logger.info("LLM 诊断成功: guid=%s, summary=%s", request.guid, llm_result.get("summary", ""))
            return DiagnoseLogResponse(
                guid=request.guid,
                summary=llm_result["summary"] or self._build_summary(request),
                possible_reason=llm_result["possible_reason"] or self._build_possible_reason(request),
                root_cause=(
                    llm_result["root_cause"]
                    or llm_result["possible_reason"]
                    or self._build_possible_reason(request)
                ),
                confidence=llm_result["confidence"] or self._build_fallback_confidence(request, knowledge_contexts),
                evidence_logs=llm_result["evidence_logs"] or self._build_evidence_logs(request),
                related_files=(
                    llm_result["related_files"]
                    or llm_result["related_code"]
                    or self._build_related_code(request)
                ),
                next_actions=(
                    llm_result["next_actions"]
                    or llm_result["suggestions"]
                    or self._build_suggestions(request)
                ),
                need_more_info=llm_result["need_more_info"] or self._build_need_more_info(request, knowledge_contexts),
                missing_info=llm_result["missing_info"] or self._build_missing_info(request, knowledge_contexts),
                manual_check_items=llm_result["manual_check_items"],
                related_code=llm_result["related_code"] or self._build_related_code(request),
                suggestions=llm_result["suggestions"] or self._build_suggestions(request),
            )
        except Exception:
            # 这里必须把异常打出来，不然前端只会看到“模拟结果”，很难判断问题出在哪。
            logger.exception("LLM 诊断失败，回退到本地模拟逻辑: guid=%s", request.guid)

        return self._build_fallback_response(request)

    def _build_fallback_response(self, request: DiagnoseLogRequest) -> DiagnoseLogResponse:
        possible_reason = self._build_possible_reason(request)
        summary = self._build_summary(request)

        return DiagnoseLogResponse(
            guid=request.guid,
            summary=summary,
            possible_reason=possible_reason,
            root_cause=possible_reason,
            confidence=self._build_fallback_confidence(request, []),
            evidence_logs=self._build_evidence_logs(request),
            related_files=self._build_related_code(request),
            next_actions=self._build_suggestions(request),
            need_more_info=self._build_need_more_info(request, []),
            missing_info=self._build_missing_info(request, []),
            manual_check_items=[],
            related_code=self._build_related_code(request),
            suggestions=self._build_suggestions(request),
        )

    def _build_summary(self, request: DiagnoseLogRequest) -> str:
        if request.exception_message:
            return f"检测到异常 {request.exception_class or 'Exception'}: {request.exception_message}"
        if request.request_uri:
            return f"检测到接口 {request.request_uri} 的异常诊断请求"
        return "检测到一条待分析的异常日志诊断请求"

    def _build_possible_reason(self, request: DiagnoseLogRequest) -> str:
        if request.exception_message:
            return f"当前为模拟诊断结果，初步判断问题与“{request.exception_message}”直接相关。"
        if request.top_stack_frame:
            return f"当前为模拟诊断结果，建议优先检查堆栈位置 {request.top_stack_frame}。"
        return "当前为模拟诊断结果，后续会在接入 LangChain 和知识库后给出更准确结论。"

    def _build_related_code(self, request: DiagnoseLogRequest) -> list[str]:
        related_code: list[str] = []
        if request.controller:
            related_code.append(request.controller)
        if request.top_stack_frame:
            related_code.append(request.top_stack_frame)
        return related_code

    def _build_suggestions(self, request: DiagnoseLogRequest) -> list[str]:
        suggestions = ["先检查 Java 侧传来的结构化日志字段是否完整。"]
        if request.exception_message:
            suggestions.append("围绕异常消息检索知识库中的相关代码和业务规则。")
        if request.top_stack_frame:
            suggestions.append("优先核对首个业务堆栈对应的方法实现。")
        suggestions.append("后续接入 LangChain 后，再叠加 RAG 代码召回和模型归因。")
        return suggestions

    def _build_evidence_logs(self, request: DiagnoseLogRequest) -> list[str]:
        evidence: list[str] = []
        if request.exception_message:
            evidence.append(f"异常信息: {request.exception_message}")
        if request.exception_class:
            evidence.append(f"异常类型: {request.exception_class}")
        if request.top_stack_frame:
            evidence.append(f"首个堆栈: {request.top_stack_frame}")
        if not evidence and request.raw_logs:
            evidence.append(request.raw_logs[0][:500])
        return evidence

    def _build_fallback_confidence(self, request: DiagnoseLogRequest, knowledge_contexts: list[str]) -> float:
        confidence = 0.35
        if request.exception_message:
            confidence += 0.15
        if request.top_stack_frame:
            confidence += 0.15
        if knowledge_contexts:
            confidence += 0.15
        return min(confidence, 0.75)

    def _build_need_more_info(self, request: DiagnoseLogRequest, knowledge_contexts: list[str]) -> bool:
        return bool(self._build_missing_info(request, knowledge_contexts))

    def _build_missing_info(self, request: DiagnoseLogRequest, knowledge_contexts: list[str]) -> list[str]:
        missing: list[str] = []
        if not request.top_stack_frame:
            missing.append("异常发生位置附近的 Service/Controller 方法源码")
        if not request.controller:
            missing.append("触发异常的 Controller 方法源码")
        if not knowledge_contexts:
            missing.append("知识库中相关 Service、Controller、Interceptor、Mapper 或异常处理器代码")
        return missing
