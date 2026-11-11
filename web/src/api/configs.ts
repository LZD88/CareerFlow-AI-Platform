import request from '../utils/request';
import type { PageResult, QueryParams } from '../types/common';
import type { SysConfigCreateReq, SysConfigEntity, SysConfigUpdateReq } from '../types/system';

export function getConfigPage(params: QueryParams) {
  return request.get<never, PageResult<SysConfigEntity>>('/api/system/configs', { params });
}

export function getConfigDetail(id: number) {
  return request.get<never, SysConfigEntity>(`/api/system/configs/${id}`);
}

export function createConfig(data: SysConfigCreateReq) {
  return request.post<never, number>('/api/system/configs', data);
}

export function updateConfig(id: number, data: SysConfigUpdateReq) {
  return request.put(`/api/system/configs/${id}`, data);
}

export function deleteConfig(id: number) {
  return request.delete(`/api/system/configs/${id}`);
}
