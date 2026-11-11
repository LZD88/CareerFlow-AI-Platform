export interface StudentResume {
  id: number;
  studentId: number;
  studentCode: string;
  studentName: string;
  resumeName: string;
  targetRole?: string;
  contentType: 'TEXT' | 'FILE';
  resumeText: string;
  resumeSummary?: string;
  resumeFileUrl?: string;
  resumeFilePreviewUrl?: string;
  parseStatus: 'PENDING' | 'SUCCESS' | 'FAILED';
  defaultResume: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface StudentResumeQueryParams {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  targetRole?: string;
  defaultResume?: boolean;
}
