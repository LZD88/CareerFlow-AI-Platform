import request from '../utils/request';
import type {
  MediaDraftSaveReq,
  MediaDraftSaveRes,
  MediaTextDraftReq,
  MediaTextDraftRes,
  MediaTopicReq,
  MediaTopicRes
} from '../types/media-agent';

export async function generateMediaTopics(data: MediaTopicReq) {
  const response = await request.post<never, MediaTopicRes>('/api/media-agent/topics', data, {
    timeout: 130000
  });
  return normalizeMediaTopicResponse(response);
}

export async function generateMediaTextDraft(data: MediaTextDraftReq) {
  const response = await request.post<never, MediaTextDraftRes>('/api/media-agent/drafts/generate-text', data, {
    timeout: 190000
  });
  return normalizeMediaTextDraftResponse(response);
}

export async function saveMediaDraft(data: MediaDraftSaveReq) {
  return request.post<never, MediaDraftSaveRes>('/api/media-agent/drafts/save', data, {
    timeout: 30000
  });
}

function normalizeMediaTopicResponse(response: MediaTopicRes): MediaTopicRes {
  return {
    ...response,
    topics: Array.isArray(response.topics)
      ? response.topics.map((item) => ({
          ...item,
          contentPoints: parseStringList((item as any).contentPoints),
          platformFit: parseStringList((item as any).platformFit),
          score: Number.isFinite(Number(item.score)) ? Number(item.score) : 0
        }))
      : []
  };
}

function normalizeMediaTextDraftResponse(response: MediaTextDraftRes): MediaTextDraftRes {
  return {
    ...response,
    xiaohongshuNote: {
      ...response.xiaohongshuNote,
      titles: parseStringList((response.xiaohongshuNote as any)?.titles),
      imagePagePlan: parseStringList((response.xiaohongshuNote as any)?.imagePagePlan),
      hashtags: parseStringList((response.xiaohongshuNote as any)?.hashtags)
    },
    douyinVideo: {
      ...response.douyinVideo,
      hooks: parseStringList((response.douyinVideo as any)?.hooks),
      shotList: parseStringList((response.douyinVideo as any)?.shotList),
      subtitles: parseStringList((response.douyinVideo as any)?.subtitles),
      hashtags: parseStringList((response.douyinVideo as any)?.hashtags)
    },
    douyinImageText: {
      ...response.douyinImageText,
      titles: parseStringList((response.douyinImageText as any)?.titles),
      imagePagePlan: parseStringList((response.douyinImageText as any)?.imagePagePlan),
      hashtags: parseStringList((response.douyinImageText as any)?.hashtags)
    }
  };
}

function parseStringList(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value.map((item) => String(item)).filter(Boolean);
  }
  if (typeof value !== 'string') {
    return [];
  }
  const text = value.trim();
  if (!text || text === '[]') {
    return [];
  }
  try {
    const parsed = JSON.parse(text);
    if (Array.isArray(parsed)) {
      return parsed.map((item) => String(item)).filter(Boolean);
    }
  } catch {
    // 兼容 Java List#toString。
  }
  if (text.startsWith('[') && text.endsWith(']')) {
    return text
      .slice(1, -1)
      .split(/,\s*/)
      .map((item) => item.trim())
      .filter(Boolean);
  }
  return [text];
}
