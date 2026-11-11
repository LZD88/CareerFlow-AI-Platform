from pydantic import BaseModel, Field


class InterviewTranscribeRequest(BaseModel):
    taskId: int = Field(..., description="Java 后端面试复盘任务 ID")
    audioObjectKey: str = Field(..., min_length=1, description="音频在对象存储中的 Key")
    audioUrl: str = Field(..., min_length=1, description="音频下载预签名地址")
    language: str = Field(default="auto", description="识别语言，默认自动识别")
    speakerDiarization: bool = Field(default=False, description="是否启用说话人分离")


class InterviewTranscriptSegment(BaseModel):
    start: float | None = Field(default=None, description="片段开始时间，单位秒")
    end: float | None = Field(default=None, description="片段结束时间，单位秒")
    speaker: int | str | None = Field(default=None, description="声纹说话人 ID")
    speakerName: str | None = Field(default=None, description="展示用说话人名称")
    text: str


class InterviewTranscribeResponse(BaseModel):
    taskId: int
    audioObjectKey: str
    text: str
    segments: list[InterviewTranscriptSegment] = Field(default_factory=list)


class InterviewReviewGenerateRequest(BaseModel):
    taskId: int = Field(..., description="Java 后端面试复盘任务 ID")
    title: str | None = Field(default=None, description="任务标题")
    candidateName: str | None = Field(default=None, description="候选人")
    interviewRole: str | None = Field(default=None, description="面试岗位")
    transcriptText: str = Field(..., min_length=1, description="面试转写文本")
    resumeFilename: str | None = Field(default=None, description="简历文件名")
    resumeObjectKey: str | None = Field(default=None, description="简历对象存储 Key")
    resumeUrl: str | None = Field(default=None, description="简历下载预签名地址")
    resumeContentType: str | None = Field(default=None, description="简历 Content-Type")


class InterviewReviewGenerateResponse(BaseModel):
    taskId: int
    reviewReport: str


class InterviewQuestionExtractRequest(BaseModel):
    taskId: int = Field(..., description="Java 后端面试复盘任务 ID")
    title: str | None = Field(default=None, description="任务标题")
    interviewRole: str | None = Field(default=None, description="面试岗位")
    transcriptText: str = Field(..., min_length=1, description="面试转写文本")
    resumeText: str | None = Field(default=None, description="已提取的简历文本")
    resumeFilename: str | None = Field(default=None, description="简历文件名")
    resumeUrl: str | None = Field(default=None, description="简历下载预签名地址")
    resumeContentType: str | None = Field(default=None, description="简历 Content-Type")


class InterviewQuestionExtractItem(BaseModel):
    originalQuestion: str
    normalizedQuestion: str
    category: str | None = None
    tags: list[str] = Field(default_factory=list)
    answerContext: str | None = None
    studentAnswerQuality: str | None = Field(default=None, description="学生回答质量：GOOD/PARTIAL/WRONG/INSUFFICIENT/UNKNOWN")
    studentAnswerScore: int | None = Field(default=None, ge=0, le=100, description="学生本次回答评分")
    studentAnswerAnalysis: str | None = Field(default=None, description="学生本次回答质量分析")
    improvementSuggestion: str | None = Field(default=None, description="下次回答改进建议")
    standardAnswer: str
    shortAnswer: str | None = None
    followUpQuestions: list[str] = Field(default_factory=list)
    pitfalls: list[str] = Field(default_factory=list)


class InterviewQuestionExtractResponse(BaseModel):
    taskId: int
    questions: list[InterviewQuestionExtractItem] = Field(default_factory=list)
