export interface MediaTopicReq {
  theme: string;
  platforms?: string[];
  audience?: string;
  goal?: string;
  style?: string;
  topicCount?: number;
}

export interface MediaTopicItem {
  topic: string;
  angle?: string;
  audience?: string;
  reason?: string;
  contentPoints?: string[];
  platformFit?: string[];
  score?: number;
}

export interface MediaTopicRes {
  theme: string;
  topics: MediaTopicItem[];
}

export interface MediaTextDraftReq {
  theme: string;
  topic: MediaTopicItem;
  style?: string;
}

export interface XiaohongshuNoteDraft {
  titles?: string[];
  coverText?: string;
  body?: string;
  imagePagePlan?: string[];
  hashtags?: string[];
}

export interface DouyinVideoDraft {
  hooks?: string[];
  script?: string;
  shotList?: string[];
  subtitles?: string[];
  hashtags?: string[];
}

export interface DouyinImageTextDraft {
  titles?: string[];
  coverText?: string;
  body?: string;
  imagePagePlan?: string[];
  hashtags?: string[];
}

export interface MediaTextDraftRes {
  theme: string;
  topic: MediaTopicItem;
  xiaohongshuNote: XiaohongshuNoteDraft;
  douyinVideo: DouyinVideoDraft;
  douyinImageText: DouyinImageTextDraft;
}

export interface MediaDraftSaveReq {
  theme: string;
  topic: MediaTopicItem;
  style?: string;
  draft: MediaTextDraftRes;
}

export interface MediaDraftSaveRes {
  projectId: number;
  draftIds: Record<string, number>;
}
