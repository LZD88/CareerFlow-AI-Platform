import request from '../utils/request';
import type { PageResult } from '../types/common';
import type {
  StudentBindPermissionTemplateReq,
  StudentEntity,
  StudentPermissionBindingDetailRes,
  StudentQueryParams
} from '../types/system';

export function getStudentPage(params: StudentQueryParams) {
  return request.get<never, PageResult<StudentEntity>>('/api/students', { params });
}

export function getStudentPermissionBindingDetail(id: number) {
  return request.get<never, StudentPermissionBindingDetailRes>(`/api/students/${id}/permission-template-detail`);
}

export function bindStudentPermissionTemplate(id: number, data: StudentBindPermissionTemplateReq) {
  return request.put(`/api/students/${id}/permission-template`, data);
}

export function unbindStudentPermissionTemplate(id: number) {
  return request.delete(`/api/students/${id}/permission-template`);
}
