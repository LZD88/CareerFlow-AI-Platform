export interface InterviewQuestionBank {
  id: number;
  normalizedQuestion: string;
  category?: string;
  tags: string[];
  standardAnswer: string;
  shortAnswer?: string;
  followUpQuestions: string[];
  pitfalls: string[];
  sourceCount: number;
  lastSourceTaskId?: number;
  createdAt: string;
  updatedAt: string;
}

export interface InterviewQuestionQueryParams {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  category?: string;
}

export interface InterviewQuestionGenerateResult {
  status?: string;
  extractedCount: number;
  createdCount: number;
  mergedCount: number;
}

export interface InterviewQuestionSource {
  id: number;
  questionId: number;
  taskId: number;
  taskTitle?: string;
  interviewRole?: string;
  originalFilename?: string;
  originalQuestion: string;
  answerContext?: string;
  studentAnswerQuality?: 'GOOD' | 'PARTIAL' | 'WRONG' | 'INSUFFICIENT' | 'UNKNOWN';
  studentAnswerScore?: number;
  studentAnswerAnalysis?: string;
  improvementSuggestion?: string;
  createdAt: string;
}
