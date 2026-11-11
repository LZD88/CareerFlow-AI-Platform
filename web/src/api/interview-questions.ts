import request from '../utils/request';
import type { PageResult } from '../types/common';
import type { InterviewQuestionBank, InterviewQuestionGenerateResult, InterviewQuestionQueryParams, InterviewQuestionSource } from '../types/interview-question';

export function getInterviewQuestionPage(params: InterviewQuestionQueryParams) {
  return request.get<never, PageResult<InterviewQuestionBank>>('/api/interview-questions', { params });
}

export function getInterviewQuestionDetail(id: number) {
  return request.get<never, InterviewQuestionBank>(`/api/interview-questions/${id}`);
}

export function getInterviewQuestionSources(id: number) {
  return request.get<never, InterviewQuestionSource[]>(`/api/interview-questions/${id}/sources`);
}

export function generateInterviewQuestionsFromTask(taskId: number) {
  return request.post<never, InterviewQuestionGenerateResult>(`/api/interview-questions/generate-from-task/${taskId}`);
}
