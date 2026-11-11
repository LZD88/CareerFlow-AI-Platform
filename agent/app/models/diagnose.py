from typing import List, Optional

from pydantic import BaseModel, ConfigDict, Field


class DiagnoseLogRequest(BaseModel):
    # Java 侧传过来的是驼峰字段，这里显式做别名映射。
    # 这样 Python 内部仍然使用更自然的 snake_case 命名，但不会丢字段。
    model_config = ConfigDict(populate_by_name=True)

    guid: str = Field(..., description="请求唯一标识")
    request_uri: Optional[str] = Field(default=None, alias="requestUri", description="请求路径")
    http_method: Optional[str] = Field(default=None, alias="httpMethod", description="HTTP 方法")
    controller: Optional[str] = Field(default=None, description="控制器方法")
    request_args: Optional[str] = Field(default=None, alias="requestArgs", description="接口开始日志中的请求参数")
    exception_class: Optional[str] = Field(default=None, alias="exceptionClass", description="异常类型")
    exception_message: Optional[str] = Field(default=None, alias="exceptionMessage", description="异常消息")
    top_stack_frame: Optional[str] = Field(default=None, alias="topStackFrame", description="首个关键堆栈")
    raw_logs: List[str] = Field(default_factory=list, alias="rawLogs", description="原始日志列表")


class DiagnoseLogResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    guid: str
    summary: str
    possible_reason: str
    root_cause: str = Field(default="", alias="rootCause")
    confidence: float = Field(default=0.0, ge=0.0, le=1.0)
    evidence_logs: List[str] = Field(default_factory=list, alias="evidenceLogs")
    related_files: List[str] = Field(default_factory=list, alias="relatedFiles")
    next_actions: List[str] = Field(default_factory=list, alias="nextActions")
    need_more_info: bool = Field(default=False, alias="needMoreInfo")
    missing_info: List[str] = Field(default_factory=list, alias="missingInfo")
    manual_check_items: List[str] = Field(default_factory=list, alias="manualCheckItems")
    related_code: List[str] = Field(default_factory=list)
    suggestions: List[str] = Field(default_factory=list)
