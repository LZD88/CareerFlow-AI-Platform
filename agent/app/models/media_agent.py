from pydantic import BaseModel, Field


class MediaTopicRequest(BaseModel):
    theme: str = Field(..., min_length=1, max_length=200, description="用户输入的创作主题")
    platforms: list[str] = Field(default_factory=lambda: ["xiaohongshu", "douyin"], description="目标平台")
    audience: str = Field(default="", max_length=200, description="目标人群")
    goal: str = Field(default="", max_length=100, description="内容目标，例如招生转化、品牌曝光、科普")
    style: str = Field(default="", max_length=100, description="内容风格，例如专业、轻松、故事、干货")
    topicCount: int = Field(default=6, ge=3, le=8, description="希望返回的选题数量")


class MediaTopicItem(BaseModel):
    topic: str = Field(default="", description="选题方向，表达内容要讲什么，不是最终标题")
    angle: str = Field(default="", description="内容角度，例如纠偏科普、痛点共鸣、故事案例")
    audience: str = Field(default="", description="该选题面向的具体用户")
    reason: str = Field(default="", description="推荐该选题的原因")
    contentPoints: list[str] = Field(default_factory=list, description="建议展开的内容要点")
    platformFit: list[str] = Field(default_factory=list, description="适配平台")
    score: int = Field(default=0, ge=0, le=100, description="推荐分")


class MediaTopicResponse(BaseModel):
    theme: str
    topics: list[MediaTopicItem]


class MediaTextDraftRequest(BaseModel):
    theme: str = Field(..., min_length=1, max_length=200, description="用户输入的创作主题")
    topic: MediaTopicItem = Field(..., description="用户选中的选题方向")
    style: str = Field(default="", max_length=100, description="内容风格")


class XiaohongshuNoteDraft(BaseModel):
    titles: list[str] = Field(default_factory=list, description="小红书标题候选")
    coverText: str = Field(default="", description="封面文案")
    body: str = Field(default="", description="图文正文")
    imagePagePlan: list[str] = Field(default_factory=list, description="图片页结构建议")
    hashtags: list[str] = Field(default_factory=list, description="话题标签")


class DouyinVideoDraft(BaseModel):
    hooks: list[str] = Field(default_factory=list, description="短视频开场钩子")
    script: str = Field(default="", description="口播脚本")
    shotList: list[str] = Field(default_factory=list, description="分镜建议")
    subtitles: list[str] = Field(default_factory=list, description="屏幕字幕关键句")
    hashtags: list[str] = Field(default_factory=list, description="话题标签")


class DouyinImageTextDraft(BaseModel):
    titles: list[str] = Field(default_factory=list, description="抖音图文标题候选")
    coverText: str = Field(default="", description="封面文案")
    body: str = Field(default="", description="图文正文")
    imagePagePlan: list[str] = Field(default_factory=list, description="图片页结构建议")
    hashtags: list[str] = Field(default_factory=list, description="话题标签")


class MediaTextDraftResponse(BaseModel):
    theme: str
    topic: MediaTopicItem
    xiaohongshuNote: XiaohongshuNoteDraft
    douyinVideo: DouyinVideoDraft
    douyinImageText: DouyinImageTextDraft
