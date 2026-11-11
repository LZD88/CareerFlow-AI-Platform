import asyncio
import logging

from fastapi import APIRouter, WebSocket, WebSocketDisconnect

from app.services.mock_interview.realtime_token import MockInterviewRealtimeTokenVerifier
from app.services.mock_interview.volc_realtime_client import VolcRealtimeInterviewClient

router = APIRouter()
logger = logging.getLogger(__name__)
token_verifier = MockInterviewRealtimeTokenVerifier()


@router.websocket("/mock-interview/realtime")
async def mock_interview_realtime(websocket: WebSocket) -> None:
    token = websocket.query_params.get("token", "")
    try:
        claims = token_verifier.verify(token)
    except Exception as exc:
        await websocket.close(code=4401, reason=str(exc)[:120])
        return

    await websocket.accept()
    volc_client = VolcRealtimeInterviewClient(claims)
    receiver_task: asyncio.Task | None = None
    try:
        await websocket.send_json({"type": "CONNECTING", "message": "正在连接实时面试模型"})
        await volc_client.connect()
        await websocket.send_json({"type": "CONNECTED", "sessionId": claims.session_id, "volcSessionId": claims.volc_session_id})
        receiver_task = asyncio.create_task(_forward_volc_to_browser(websocket, volc_client))
        await _forward_browser_to_volc(websocket, volc_client)
    except WebSocketDisconnect:
        logger.info("模拟面试前端断开 session_id=%s", claims.session_id)
    except Exception as exc:
        logger.exception("模拟面试实时网关异常 session_id=%s", claims.session_id)
        await _send_error(websocket, str(exc))
    finally:
        if receiver_task:
            receiver_task.cancel()
        await volc_client.finish()


async def _forward_volc_to_browser(websocket: WebSocket, volc_client: VolcRealtimeInterviewClient) -> None:
    async for frame in volc_client.frames():
        event = volc_client.to_frontend_event(frame)
        if event["type"] == "AUDIO":
            await websocket.send_bytes(frame.payload)
        else:
            await websocket.send_json(event)


async def _forward_browser_to_volc(websocket: WebSocket, volc_client: VolcRealtimeInterviewClient) -> None:
    while True:
        message = await websocket.receive()
        if message.get("bytes") is not None:
            await volc_client.send_audio(message["bytes"])
            continue

        text = message.get("text")
        if text is None:
            continue

        if text == "__end_asr__":
            await volc_client.end_asr()
            continue
        if text == "__finish__":
            break
        await volc_client.send_text_query(text)


async def _send_error(websocket: WebSocket, message: str) -> None:
    try:
        await websocket.send_json({"type": "ERROR", "message": message})
    except Exception:
        logger.debug("发送模拟面试错误事件失败", exc_info=True)
