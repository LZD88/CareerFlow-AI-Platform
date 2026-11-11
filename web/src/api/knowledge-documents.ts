import request from '../utils/request';
import type { PageResult } from '../types/common';
import type {
  KnowledgeDocumentChunk,
  KnowledgeDocumentCreateReq,
  KnowledgeDocumentEntity,
  KnowledgeDocumentUploadRes
} from '../types/knowledge-base';

export interface KnowledgeDocumentQueryParams {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: string;
  courseId?: number;
  studyMode?: string;
  sourceType?: string;
  permissionScope?: string;
}

export function getKnowledgeDocumentPage(params: KnowledgeDocumentQueryParams) {
  return request.get<never, PageResult<KnowledgeDocumentEntity>>('/api/knowledge/documents', { params });
}

export function getKnowledgeDocumentDetail(id: number) {
  return request.get<never, KnowledgeDocumentEntity>(`/api/knowledge/documents/${id}`);
}

export function getKnowledgeDocumentChunks(id: number, params: { pageNum?: number; pageSize?: number; includeVector?: boolean }) {
  return request.get<never, PageResult<KnowledgeDocumentChunk>>(`/api/knowledge/documents/${id}/chunks`, { params });
}

export function createKnowledgeDocument(data: KnowledgeDocumentCreateReq) {
  return request.post<never, number>('/api/knowledge/documents', data, {
    timeout: 70000
  });
}

export function uploadMarkdownKnowledgeDocuments(
  files: File[],
  indexNow: boolean,
  options?: { sourceType?: string; permissionScope?: string }
) {
  const formData = new FormData();
  files.forEach((file) => formData.append('files', file));
  formData.append('indexNow', String(indexNow));
  if (options?.sourceType) {
    formData.append('sourceType', options.sourceType);
  }
  if (options?.permissionScope) {
    formData.append('permissionScope', options.permissionScope);
  }
  return request.post<never, KnowledgeDocumentUploadRes>('/api/knowledge/documents/upload-md', formData, {
    timeout: 180000,
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

export function uploadJsonKnowledgeDocuments(
  files: File[],
  indexNow: boolean,
  options?: { sourceType?: string; permissionScope?: string }
) {
  const formData = new FormData();
  files.forEach((file) => formData.append('files', file));
  formData.append('indexNow', String(indexNow));
  if (options?.sourceType) {
    formData.append('sourceType', options.sourceType);
  }
  if (options?.permissionScope) {
    formData.append('permissionScope', options.permissionScope);
  }
  return request.post<never, KnowledgeDocumentUploadRes>('/api/knowledge/documents/upload-json', formData, {
    timeout: 180000,
    headers: { 'Content-Type': 'multipart/form-data' }
  });
}

export function updateKnowledgeDocument(id: number, data: KnowledgeDocumentCreateReq) {
  return request.post<never, void>(`/api/knowledge/documents/${id}/update`, data, {
    timeout: 70000
  });
}

export function reindexKnowledgeDocument(id: number) {
  return request.post(`/api/knowledge/documents/${id}/reindex`, undefined, {
    timeout: 70000
  });
}

export function disableKnowledgeDocument(id: number) {
  return request.post(`/api/knowledge/documents/${id}/disable`);
}
