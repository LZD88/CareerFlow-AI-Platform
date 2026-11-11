export interface KnowledgeDocumentEntity {
  id: number;
  documentCode: string;
  title: string;
  content: string;
  sourceType: string;
  sourcePath: string;
  courseId?: number;
  classId?: number;
  studyMode?: 'ALL' | 'ONLINE' | 'OFFLINE';
  version: number;
  permissionScope: string;
  status: 'PENDING' | 'INDEXING' | 'INDEXED' | 'FAILED' | 'DISABLED';
  chunkCount: number;
  errorMessage?: string;
  indexedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface KnowledgeDocumentCreateReq {
  title: string;
  content: string;
  courseId?: number;
  studyMode?: 'ALL' | 'ONLINE' | 'OFFLINE';
  sourceType?: string;
  sourcePath?: string;
  permissionScope?: string;
}

export interface KnowledgeDocumentChunk {
  id: string;
  documentId?: string;
  chunkId?: string;
  courseId?: number;
  classId?: number;
  studyMode?: 'ALL' | 'ONLINE' | 'OFFLINE';
  permissionScope?: string;
  title?: string;
  headerPath?: string;
  content?: string;
  contentLength?: number;
  version?: number;
  sourceType?: string;
  sourcePath?: string;
  vectorDim?: number;
  vectorPreview?: number[];
  vector?: number[];
  knowledgeDomain?: string;
  chunkType?: string;
  methodName?: string;
  methodHeading?: string;
  tableName?: string;
}

export interface KnowledgeDocumentUploadItem {
  fileName: string;
  documentId?: number;
  documentCode?: string;
  status?: string;
  chunkCount?: number;
  errorMessage?: string;
}

export interface KnowledgeDocumentUploadRes {
  total: number;
  success: number;
  failed: number;
  indexNow: boolean;
  items: KnowledgeDocumentUploadItem[];
}
