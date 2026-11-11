import request from '../utils/request';
import type { PageResult, QueryParams } from '../types/common';
import type {
  PermissionEntity,
  RoleCreateReq,
  RoleDetailRes,
  RoleEntity,
  RoleUpdateReq
} from '../types/system';

export function getRolePage(params: QueryParams) {
  return request.get<never, PageResult<RoleEntity>>('/api/system/roles', { params });
}

export function getRoleDetail(id: number) {
  return request.get<never, RoleDetailRes>(`/api/system/roles/${id}`);
}

export function createRole(data: RoleCreateReq) {
  return request.post<never, number>('/api/system/roles', data);
}

export function updateRole(id: number, data: RoleUpdateReq) {
  return request.put(`/api/system/roles/${id}`, data);
}

export function deleteRole(id: number) {
  return request.delete(`/api/system/roles/${id}`);
}

export function assignRolePermissions(id: number, permissionIds: number[]) {
  return request.put(`/api/system/roles/${id}/permissions`, { permissionIds });
}

export function getPermissionOptions() {
  return request.get<never, PageResult<PermissionEntity>>('/api/system/permissions', {
    params: { pageNum: 1, pageSize: 1000 }
  });
}
