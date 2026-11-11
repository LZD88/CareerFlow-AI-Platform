import json
import logging

from fastapi import APIRouter, Request
from fastapi.responses import StreamingResponse
from pydantic import ValidationError

from app.models.student_chat import StudentChatStreamRequest, StudentChatSummarizeRequest, StudentChatSummarizeResponse
from app.services.student_chat.student_chat_service import StudentChatService

router = APIRouter()
service = StudentChatService()
logger = logging.getLogger(__name__)


@router.post("/student-chat/stream")
async def stream_student_chat(http_request: Request) -> StreamingResponse:
    """
    Java 后端调用的学生问答流式接口。

    注意：这个接口不是直接暴露给前端的学生端接口。
    前端仍然只连 Java，Java 完成鉴权、会话落库和 SSE 转发。
    Python 只负责 AI 计算，并用一行一个 JSON 的 NDJSON 流返回事件。

    这里不直接把参数声明成 StudentChatStreamRequest，而是先读取原始 body 再手动解析。
    原因是 Java/Python 流式联调阶段，如果字段名、类型或 Content-Type 有一点偏差，
    FastAPI 会在进入函数前直接返回 422，Java 侧只能看到 Unprocessable Entity。
    手动解析可以把真实请求体打到日志里，并在解析失败时降级成空请求，方便联调。
    """
    request = await parse_request_body(http_request)
    return StreamingResponse(
        service.stream_chat(request),
        media_type="application/x-ndjson; charset=utf-8",
    )


@router.post("/student-chat/summarize", response_model=StudentChatSummarizeResponse)
def summarize_student_chat(request: StudentChatSummarizeRequest) -> dict:
    """
    Java 后端异步调用的会话摘要接口。

    前端不直接访问该接口；Python 只负责生成摘要文本。
    """
    return service.summarize(request)


async def parse_request_body(http_request: Request) -> StudentChatStreamRequest:
    body = await http_request.body()
    if not body:
        logger.warning("学生问答 Python stream 收到空请求体")
        return StudentChatStreamRequest()

    raw_text = body.decode("utf-8", errors="replace")
    try:
        payload = json.loads(raw_text)
    except json.JSONDecodeError:
        logger.warning("学生问答 Python stream 请求体不是 JSON: body=%s", raw_text[:1000])
        return StudentChatStreamRequest()

    try:
        return StudentChatStreamRequest.model_validate(payload)
    except ValidationError as ex:
        logger.warning("学生问答 Python stream 请求体结构不匹配: body=%s, errors=%s", raw_text[:1000], ex.errors())
        # 保底提取 question.content，避免因为某个非关键字段类型不一致导致整条流失败。
        question = payload.get("question") if isinstance(payload, dict) else None
        content = question.get("content") if isinstance(question, dict) else ""
        return StudentChatStreamRequest(question={"content": content or ""})
