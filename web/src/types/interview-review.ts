export type InterviewReviewStatus = 'UPLOADED' | 'TRANSCRIBING' | 'TRANSCRIBED' | 'REVIEWING' | 'DONE' | 'FAILED';
export type InterviewQuestionStatus = 'NONE' | 'PROCESSING' | 'DONE' | 'FAILED';

export interface InterviewReviewTask {
  id: number;
  title: string;
  candidateName?: string;
  interviewRole?: string;
  originalFilename: string;
  audioObjectKey: string;
  audioUrl: string;
  audioPreviewUrl?: string;
  audioContentType?: string;
  audioSize: number;
  resumeFilename?: string;
  resumeObjectKey?: string;
  resumeUrl?: string;
  resumePreviewUrl?: string;
  resumeContentType?: string;
  resumeSize?: number;
  status: InterviewReviewStatus;
  transcriptText?: string;
  reviewReport?: string;
  failReason?: string;
  questionStatus?: InterviewQuestionStatus;
  questionExtractedCount?: number;
  questionCreatedCount?: number;
  questionMergedCount?: number;
  questionFailReason?: string;
  createdBy?: number;
  createdAt: string;
  updatedAt: string;
}

export interface InterviewReviewQueryParams {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: InterviewReviewStatus;
}

export interface InterviewReviewUploadValues {
  title?: string;
  candidateName?: string;
  interviewRole?: string;
}

export interface UploadPresignRes {
  objectKey: string;
  uploadUrl: string;
  fileUrl: string;
  headers?: Record<string, string>;
}
