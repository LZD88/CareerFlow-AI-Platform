import logging
import time

from fastapi import Request
from fastapi import FastAPI

from app.api.router import api_router
from app.config.settings import get_settings

# 统一初始化 Python 服务日志。
# 这样在 PyCharm / VS Code / 终端里都能稳定看到我们自己打的 info、error 日志，
# 不会只剩下 uvicorn 的访问日志。
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
    force=True,
)
logger = logging.getLogger(__name__)

settings = get_settings()

app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description="Python agent service for structured log diagnosis.",
)
app.include_router(api_router)
logger.info("Python Agent 启动完成: app=%s version=%s", settings.app_name, settings.app_version)


@app.middleware("http")
async def log_interview_review_request(request: Request, call_next):
    if "/api/interview-review/" not in request.url.path:
        return await call_next(request)
    started_at = time.perf_counter()
    logger.info("收到 HTTP 请求: method=%s path=%s", request.method, request.url.path)
    response = await call_next(request)
    cost_ms = int((time.perf_counter() - started_at) * 1000)
    logger.info("HTTP 请求结束: method=%s path=%s status=%s cost_ms=%s", request.method, request.url.path, response.status_code, cost_ms)
    return response


@app.get("/health", tags=["system"])
def health() -> dict[str, str]:
    return {"status": "ok", "service": settings.app_name}
