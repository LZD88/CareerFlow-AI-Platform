import request from '../utils/request';
import type { PageResult } from '../types/common';
import type {
  StudentPermissionTemplateCreateReq,
  StudentPermissionTemplateDetailRes,
  StudentPermissionTemplateEntity,
  StudentPermissionTemplateQueryParams,
  StudentPermissionTemplateUpdateReq
} from '../types/system';

export function getStudentPermissionTemplatePage(params: StudentPermissionTemplateQueryParams) {
  return request.get<never, PageResult<StudentPermissionTemplateEntity>>('/api/system/student-permission-templates', { params });
}

export function getStudentPermissionTemplateDetail(id: number) {
  return request.get<never, StudentPermissionTemplateDetailRes>(`/api/system/student-permission-templates/${id}`);
}

export function createStudentPermissionTemplate(data: StudentPermissionTemplateCreateReq) {
  return request.post<never, number>('/api/system/student-permission-templates', data);
}

export function updateStudentPermissionTemplate(id: number, data: StudentPermissionTemplateUpdateReq) {
  return request.put(`/api/system/student-permission-templates/${id}`, data);
}

export function deleteStudentPermissionTemplate(id: number) {
  return request.delete(`/api/system/student-permission-templates/${id}`);
}

export function assignStudentPermissionTemplatePermissions(id: number, permissionIds: number[]) {
  return request.put(`/api/system/student-permission-templates/${id}/permissions`, { permissionIds });
}
