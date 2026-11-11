import request from '../utils/request';
import type { PageResult, QueryParams } from '../types/common';
import type {
  PermissionCreateReq,
  PermissionEntity,
  PermissionTreeRes,
  StudentPermissionTreeRes,
  PermissionUpdateReq
} from '../types/system';

export function getPermissionPage(params: QueryParams & { permissionType?: string }) {
  return request.get<never, PageResult<PermissionEntity>>('/api/system/permissions', { params });
}

export function getPermissionTree() {
  return request.get<never, PermissionTreeRes[]>('/api/system/permissions/tree');
}

export function getStudentPermissionTree() {
  return request.get<never, StudentPermissionTreeRes[]>('/api/system/permissions/student/tree');
}

export function getPermissionDetail(id: number) {
  return request.get<never, PermissionEntity>(`/api/system/permissions/${id}`);
}

export function createPermission(data: PermissionCreateReq) {
  return request.post<never, number>('/api/system/permissions', data);
}

export function updatePermission(id: number, data: PermissionUpdateReq) {
  return request.put(`/api/system/permissions/${id}`, data);
}

export function deletePermission(id: number) {
  return request.delete(`/api/system/permissions/${id}`);
}
