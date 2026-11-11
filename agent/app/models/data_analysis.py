from pydantic import BaseModel, Field
from typing import Any


class DataAnalysisTableCatalogItem(BaseModel):
    table: str = Field(..., min_length=1, description="表名")
    business_name: str = Field(default="", description="业务名称")
    desc: str = Field(default="", description="表用途说明")
    keywords: list[str] = Field(default_factory=list, description="业务关键词")
    relations: list[str] = Field(default_factory=list, description="关联关系摘要")
    common_metrics: list[str] = Field(default_factory=list, description="常用指标名称")


class DataAnalysisPlanRequest(BaseModel):
    question: str = Field(..., min_length=1, description="用户自然语言数据分析问题")
    tableCatalog: list[DataAnalysisTableCatalogItem] = Field(default_factory=list, description="第一阶段轻量表目录")


class DataAnalysisSchemaRequest(BaseModel):
    tables: list[str] = Field(..., min_length=1, description="需要精确取回 schema 的表名列表")


class DataAnalysisSchemaItem(BaseModel):
    table: str
    title: str = ""
    content: str
    documentId: str | None = None
    chunkId: str | None = None
    version: int | None = None


class DataAnalysisSchemaResponse(BaseModel):
    schemas: list[DataAnalysisSchemaItem]
    missingTables: list[str] = Field(default_factory=list)


class DataAnalysisMetricDimension(BaseModel):
    name: str = ""
    field: str = ""
    joinPath: list[str] = Field(default_factory=list)


class DataAnalysisMetricClarificationRule(BaseModel):
    when: str = ""
    ask: str = ""


class DataAnalysisMetricDefinition(BaseModel):
    code: str = Field(..., min_length=1, description="指标唯一编码")
    title: str = Field(..., min_length=1, description="指标名称")
    type: str = Field(default="atomic", description="指标类型，例如 atomic/derived")
    keywords: list[str] = Field(default_factory=list)
    businessDefinition: str = ""
    formula: str = ""
    alternativeFormulas: list[dict[str, Any]] = Field(default_factory=list)
    defaultFilters: list[str] = Field(default_factory=list)
    defaultTimeField: str = ""
    tables: list[str] = Field(default_factory=list)
    commonDimensions: list[DataAnalysisMetricDimension] = Field(default_factory=list)
    notes: list[str] = Field(default_factory=list)
    clarificationRules: list[DataAnalysisMetricClarificationRule] = Field(default_factory=list)
    examples: list[str] = Field(default_factory=list)
    version: int = Field(default=1, ge=1)
    status: str = "ACTIVE"


class DataAnalysisMetricIngestRequest(BaseModel):
    metrics: list[DataAnalysisMetricDefinition] = Field(..., min_length=1, description="指标定义列表")
    documentId: str = Field(default="", description="外层业务文档ID，知识库管理页用它关联 Chunk 详情")
    title: str = Field(default="", description="外层业务文档标题")
    sourcePath: str = Field(default="", description="来源路径")


class DataAnalysisMetricItem(BaseModel):
    code: str
    title: str = ""
    type: str = ""
    content: str = ""
    tables: list[str] = Field(default_factory=list)
    score: float = 0
    documentId: str | None = None
    chunkId: str | None = None
    version: int | None = None


class DataAnalysisMetricIngestResponse(BaseModel):
    ok: bool
    collection: str
    count: int
    metrics: list[DataAnalysisMetricItem] = Field(default_factory=list)


class DataAnalysisMetricSearchRequest(BaseModel):
    question: str = Field(..., min_length=1, description="用户自然语言数据分析问题")
    topK: int = Field(default=3, ge=1, le=10)


class DataAnalysisMetricSearchResponse(BaseModel):
    ok: bool
    question: str
    collection: str
    resultCount: int
    metrics: list[DataAnalysisMetricItem]


class DataAnalysisPlanResponse(BaseModel):
    candidateTables: list[str]
    reason: str = ""


class DataAnalysisPrepareResponse(DataAnalysisPlanResponse):
    schemas: list[DataAnalysisSchemaItem] = Field(default_factory=list)
    missingTables: list[str] = Field(default_factory=list)


class DataAnalysisGenerateSqlRequest(BaseModel):
    question: str = Field(..., min_length=1, description="用户自然语言数据分析问题")
    tableCatalog: list[DataAnalysisTableCatalogItem] = Field(default_factory=list, description="第一阶段轻量表目录")
    maxRows: int = Field(default=100, ge=1, le=500, description="SQL 默认最大返回行数")


class DataAnalysisSqlPlan(BaseModel):
    sql: str = ""
    reason: str = ""
    usedTables: list[str] = Field(default_factory=list)
    assumptions: list[str] = Field(default_factory=list)
    warnings: list[str] = Field(default_factory=list)


class DataAnalysisGenerateSqlResponse(BaseModel):
    question: str
    candidateTables: list[str]
    sql: str = ""
    reason: str = ""
    needsClarification: bool = False
    clarificationQuestions: list[str] = Field(default_factory=list)
    knowledgeMissing: bool = False
    knowledgeMissingItems: list[str] = Field(default_factory=list)
    assumptions: list[str] = Field(default_factory=list)
    warnings: list[str] = Field(default_factory=list)
    missingTables: list[str] = Field(default_factory=list)


class DataAnalysisChartSuggestion(BaseModel):
    chartType: str = Field(default="", description="建议图表类型，例如 bar/line/pie/table")
    xField: str = Field(default="", description="X 轴或分类字段")
    yField: str = Field(default="", description="Y 轴或数值字段")
    seriesField: str = Field(default="", description="系列分组字段")
    reason: str = Field(default="", description="推荐该图表的原因")


class DataAnalysisInterpretResultRequest(BaseModel):
    question: str = Field(..., min_length=1, description="用户自然语言数据分析问题")
    sql: str = Field(..., min_length=1, description="Java 实际执行的 SQL")
    columns: list[str] = Field(default_factory=list, description="查询结果列名")
    rows: list[dict] = Field(default_factory=list, description="查询结果样本或聚合结果")
    rowCount: int = Field(default=0, ge=0, description="Java 返回给 Python 的行数")
    truncated: bool = Field(default=False, description="结果是否被 Java 截断")
    assumptions: list[str] = Field(default_factory=list, description="SQL 生成阶段的口径假设")
    warnings: list[str] = Field(default_factory=list, description="SQL 生成或查询阶段的警告")


class DataAnalysisInterpretResultResponse(BaseModel):
    analysis: str = ""
    insights: list[str] = Field(default_factory=list)
    followUpQuestions: list[str] = Field(default_factory=list)
    chartSuggestion: DataAnalysisChartSuggestion | None = None
