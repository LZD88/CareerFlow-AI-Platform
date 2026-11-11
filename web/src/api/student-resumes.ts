import axios from 'axios';
import request from '../utils/request';
import type { PageResult } from '../types/common';
import type { StudentResume, StudentResumeQueryParams } from '../types/student-resume';
import { getAccessToken } from '../utils/auth';
import { apiBaseURL } from '../utils/request';
import { buildSignedHeaders } from '../utils/signature';

export function getStudentResumePage(params: StudentResumeQueryParams) {
  return request.get<never, PageResult<StudentResume>>('/api/student-resumes', { params });
}

export function getStudentResumeDetail(id: number) {
  return request.get<never, StudentResume>(`/api/student-resumes/${id}`);
}

export function deleteStudentResume(id: number) {
  return request.delete<never, void>(`/api/student-resumes/${id}`);
}

export async function getStudentResumeFilePreview(id: number) {
  const url = `/api/student-resumes/${id}/file/preview`;
  const token = getAccessToken();
  const signHeaders = await buildSignedHeaders({ method: 'GET', baseURL: apiBaseURL, url });
  const response = await axios.get<Blob>(`${apiBaseURL}${url}`, {
    responseType: 'blob',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...signHeaders
    }
  });
  return response.data;
}
