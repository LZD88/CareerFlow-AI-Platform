from pydantic import BaseModel, Field


class KnowledgeIngestTextRequest(BaseModel):
    documentId: str = Field(..., min_length=1, description="业务文档 ID，由 Java 或调用方生成")
    title: str = Field(default="", description="文档标题")
    content: str = Field(..., min_length=1, description="需要入库的文本内容")
    courseId: int | None = Field(default=None, description="课程 ID")
    classId: int | None = Field(default=None, description="班级 ID")
    studyMode: str = Field(default="ALL", description="适用学员范围：ALL / ONLINE / OFFLINE")
    version: int = Field(default=1, ge=1, description="文档版本")
    sourceType: str = Field(default="manual", description="来源类型，例如 markdown/txt/manual")
    sourcePath: str = Field(default="", description="来源路径")
    permissionScope: str = Field(default="course", description="权限范围，第一版只做元数据保留")
    knowledgeDomain: str = Field(default="STUDENT_QA", description="知识域，例如 STUDENT_QA / LOG_DIAGNOSIS")


class KnowledgeChunkIngestResult(BaseModel):
    id: str
    chunkId: str
    contentLength: int


class KnowledgeIngestTextResponse(BaseModel):
    ok: bool
    documentId: str
    version: int
    collection: str
    chunkCount: int
    chunks: list[KnowledgeChunkIngestResult]


class KnowledgeSearchRequest(BaseModel):
    query: str = Field(..., min_length=1, description="检索问题")
    courseId: int | None = Field(default=None, description="课程 ID，传入后用于过滤")
    classId: int | None = Field(default=None, description="班级 ID，传入后用于过滤")
    studyMode: str | None = Field(default=None, description="学员学习方式：ONLINE / OFFLINE")
    documentId: str | None = Field(default=None, description="文档 ID，传入后用于过滤")
    topK: int = Field(default=5, ge=1, le=20, description="返回结果数量")


class KnowledgeSearchHit(BaseModel):
    id: str
    score: float
    documentId: str | None = None
    chunkId: str | None = None
    courseId: int | None = None
    classId: int | None = None
    studyMode: str | None = None
    title: str | None = None
    content: str | None = None
    version: int | None = None
    sourceType: str | None = None
    sourcePath: str | None = None
    knowledgeDomain: str | None = None
    chunkType: str | None = None
    methodName: str | None = None
    methodHeading: str | None = None
    tableName: str | None = None


class KnowledgeSearchResponse(BaseModel):
    ok: bool
    query: str
    collection: str
    topK: int
    resultCount: int
    results: list[KnowledgeSearchHit]


class KnowledgeDocumentChunksRequest(BaseModel):
    documentId: str = Field(..., min_length=1, description="业务文档 ID")
    version: int | None = Field(default=None, ge=1, description="文档版本，不传则查询该文档全部版本")
    knowledgeDomain: str = Field(default="STUDENT_QA", description="知识域，例如 STUDENT_QA / LOG_DIAGNOSIS")
    pageNum: int = Field(default=1, ge=1, description="页码")
    pageSize: int = Field(default=20, ge=1, le=100, description="每页数量")
    includeVector: bool = Field(default=False, description="是否返回完整向量，默认只返回向量预览")


class KnowledgeDocumentChunkHit(BaseModel):
    id: str
    documentId: str | None = None
    chunkId: str | None = None
    courseId: int | None = None
    classId: int | None = None
    studyMode: str | None = None
    permissionScope: str | None = None
    title: str | None = None
    headerPath: str | None = None
    content: str | None = None
    contentLength: int = 0
    version: int | None = None
    sourceType: str | None = None
    sourcePath: str | None = None
    knowledgeDomain: str | None = None
    chunkType: str | None = None
    methodName: str | None = None
    methodHeading: str | None = None
    tableName: str | None = None
    vectorDim: int = 0
    vectorPreview: list[float] = []
    vector: list[float] | None = None


class KnowledgeDocumentChunksResponse(BaseModel):
    ok: bool
    documentId: str
    version: int | None = None
    collection: str
    total: int
    pageNum: int
    pageSize: int
    records: list[KnowledgeDocumentChunkHit]


class KnowledgeDeleteDocumentChunksRequest(BaseModel):
    documentId: str = Field(..., min_length=1, description="业务文档 ID")
    version: int | None = Field(default=None, ge=1, description="文档版本，不传则删除该文档全部版本")
    knowledgeDomain: str = Field(default="STUDENT_QA", description="知识域，例如 STUDENT_QA / LOG_DIAGNOSIS")


class KnowledgeDeleteDocumentChunksResponse(BaseModel):
    ok: bool
    documentId: str
    version: int | None = None
    collection: str
    deletedCount: int
