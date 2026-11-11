from pydantic import BaseModel, Field


class ChatMessageContext(BaseModel):
    id: int | None = Field(default=None, description="消息 ID")
    role: str = Field(default="", description="消息角色，user/assistant/system")
    content: str = Field(default="", description="消息正文")
    createdAt: str | None = Field(default=None, description="消息创建时间")


class QuestionContext(BaseModel):
    content: str = Field(default="", description="当前用户问题")
    createdAt: str | None = Field(default=None, description="问题创建时间")


class MemoryContext(BaseModel):
    summary: str = Field(default="", description="会话长期摘要")
    recentMessages: list[ChatMessageContext] = Field(
        default_factory=list,
        description="最近几轮原始对话",
    )


class GenerationContext(BaseModel):
    stream: bool = Field(default=True, description="是否流式生成")
    maxTokens: int = Field(default=2000, description="最大输出 token 数")


class StudentContext(BaseModel):
    studentId: int | None = Field(default=None, description="学生 ID")
    studentCode: str = Field(default="", description="学生编号")
    studentName: str = Field(default="", description="学生姓名")
    studyMode: str = Field(default="", description="学习方式，ONLINE/OFFLINE")
    classId: int | None = Field(default=None, description="班级 ID")
    className: str = Field(default="", description="班级名称")
    courseId: int | None = Field(default=None, description="课程 ID")
    courseName: str = Field(default="", description="课程名称")


class StudentChatStreamRequest(BaseModel):
    """
    Java 发给 Python 的学生问答流式请求。

    这里刻意使用 camelCase 字段名，和 Java JSON 保持完全一致。
    这样可以避免 Pydantic alias 在不同版本下的兼容差异，也让 422 问题更容易排查。
    """

    requestId: str = Field(default="", description="Java 侧生成的请求 ID，便于串日志")
    sessionId: int | None = Field(default=0, description="会话 ID")
    userMessageId: int | None = Field(default=0, description="Java 已保存的用户消息 ID")
    student: StudentContext = Field(default_factory=StudentContext, description="学生上下文，用于后续 RAG 过滤")
    question: QuestionContext = Field(default_factory=QuestionContext)
    memory: MemoryContext = Field(default_factory=MemoryContext)
    generation: GenerationContext = Field(default_factory=GenerationContext)


class StudentChatSummarizeRequest(BaseModel):
    """
    Java 发给 Python 的会话摘要请求。

    Java 负责判断何时压缩和更新数据库，Python 只负责调用模型生成适合后续问答使用的长期摘要。
    """

    sessionId: int | None = Field(default=None, description="会话 ID")
    previousSummary: str = Field(default="", description="已有长期摘要")
    messages: list[ChatMessageContext] = Field(default_factory=list, description="本次需要压缩的历史消息")
    maxSummaryTokens: int = Field(default=800, ge=100, le=2000, description="摘要最大 token 数")


class StudentChatSummarizeResponse(BaseModel):
    ok: bool
    summary: str
    summaryTokenCount: int
    compressedUntilMessageId: int | None = None
