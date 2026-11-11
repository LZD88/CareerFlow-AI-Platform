from fastapi import APIRouter

from app.models.diagnose import DiagnoseLogRequest, DiagnoseLogResponse
from app.services.log_diagnosis.diagnose_service import DiagnoseService

# 创建一个 FastAPI 路由分组对象。
# 这个文件里所有日志诊断相关接口，都会先挂到这个 router 上，
# 再由 app/api/router.py 统一挂到主应用。
router = APIRouter()

# 创建日志诊断业务服务对象。
# route 层只负责接收 HTTP 请求和返回 HTTP 响应，
# 具体的诊断流程交给 DiagnoseService 处理。
service = DiagnoseService()


# 声明一个 POST 接口。
# 最终完整路径是 /api/diagnose-log，因为 app/api/router.py 里给这个 router 加了 prefix="/api"。
# response_model 表示接口返回值要按 DiagnoseLogResponse 的结构输出。
@router.post("/diagnose-log", response_model=DiagnoseLogResponse)
def diagnose_log(request: DiagnoseLogRequest) -> DiagnoseLogResponse:
    # FastAPI 会自动把请求 JSON 解析成 DiagnoseLogRequest 对象。
    # 这里把解析后的请求交给业务服务，拿到诊断结果后直接返回。
    return service.diagnose(request)
