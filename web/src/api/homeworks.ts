import request from '../utils/request';
import type { PageResult } from '../types/common';
import type {
  HomeworkAnswerManageReq,
  HomeworkAssignmentAdminItem,
  HomeworkAssignmentQueryParams,
  HomeworkAssignmentSaveReq,
  HomeworkSubmissionAdminItem,
  HomeworkSubmissionAdminQueryParams,
  HomeworkSubmissionReviewReq,
  HomeworkAssignmentStatusUpdateReq,
  HomeworkPublishContextRes
} from '../types/system';

export function getHomeworkPage(params: HomeworkAssignmentQueryParams) {
  return request.get<never, PageResult<HomeworkAssignmentAdminItem>>('/api/teaching/homeworks', { params });
}

export function getHomeworkDetail(id: number) {
  return request.get<never, HomeworkAssignmentAdminItem>(`/api/teaching/homeworks/${id}`);
}

export function getHomeworkPublishContext(classId: number, homeworkDate: string) {
  return request.get<never, HomeworkPublishContextRes>('/api/teaching/homeworks/publish-context', {
    params: { classId, homeworkDate }
  });
}

export function createHomework(data: HomeworkAssignmentSaveReq) {
  return request.post<never, number>('/api/teaching/homeworks', data);
}

export function updateHomework(id: number, data: HomeworkAssignmentSaveReq) {
  return request.put(`/api/teaching/homeworks/${id}`, data);
}

export function updateHomeworkStatus(id: number, data: HomeworkAssignmentStatusUpdateReq) {
  return request.put(`/api/teaching/homeworks/${id}/status`, data);
}

export function manageHomeworkAnswer(id: number, data: HomeworkAnswerManageReq) {
  return request.put(`/api/teaching/homeworks/${id}/answer`, data);
}

export function getHomeworkSubmissionPage(id: number, params: HomeworkSubmissionAdminQueryParams) {
  return request.get<never, PageResult<HomeworkSubmissionAdminItem>>(`/api/teaching/homeworks/${id}/submissions`, { params });
}

export function reviewHomeworkSubmission(id: number, submissionId: number, data: HomeworkSubmissionReviewReq) {
  return request.put(`/api/teaching/homeworks/${id}/submissions/${submissionId}/review`, data);
}

export function deleteHomework(id: number) {
  return request.delete(`/api/teaching/homeworks/${id}`);
}

export function uploadHomeworkFile(file: File, path: string) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('path', path);
  return request.post<never, string>('/api/upload/file', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}
