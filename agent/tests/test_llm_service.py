from app.models.diagnose import DiagnoseLogRequest
from app.services.log_diagnosis.llm_service import LlmService


def test_build_context_includes_request_args() -> None:
    context = LlmService()._build_context(
        DiagnoseLogRequest(
            guid="g-1",
            requestUri="/api/auth/login",
            httpMethod="POST",
            controller="AuthController.login",
            requestArgs='[{"name":"request","value":{"username":"admin","password":"****"}}]',
            exceptionClass="BusinessException",
            exceptionMessage="用户名或密码错误",
            topStackFrame="cn.yq.modules.auth.service.impl.AuthServiceImpl.login(AuthServiceImpl.java:45)",
        )
    )

    assert 'request_args: [{"name":"request","value":{"username":"admin","password":"****"}}]' in context


def test_build_context_marks_missing_request_args_as_unknown() -> None:
    context = LlmService()._build_context(DiagnoseLogRequest(guid="g-2"))

    assert "request_args: 未知" in context
