import request from '../utils/request';
import type { PageResult, QueryParams } from '../types/common';
import type { RoleEntity, UserCreateReq, UserDetailRes, UserEntity, UserUpdateReq } from '../types/system';

export function getUserPage(params: QueryParams) {
  return request.get<never, PageResult<UserEntity>>('/api/system/users', { params });
}

export function getUserDetail(id: number) {
  return request.get<never, UserDetailRes>(`/api/system/users/${id}`);
}

export function createUser(data: UserCreateReq) {
  return request.post<never, number>('/api/system/users', data);
}

export function updateUser(id: number, data: UserUpdateReq) {
  return request.put(`/api/system/users/${id}`, data);
}

export function deleteUser(id: number) {
  return request.delete(`/api/system/users/${id}`);
}

export function assignUserRoles(id: number, roleIds: number[]) {
  return request.put(`/api/system/users/${id}/roles`, { roleIds });
}

export function getRoleOptions() {
  return request.get<never, PageResult<RoleEntity>>('/api/system/roles', {
    params: { pageNum: 1, pageSize: 1000 }
  });
}
