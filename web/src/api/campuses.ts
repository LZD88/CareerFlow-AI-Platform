import request from '../utils/request';
import type { PageResult } from '../types/common';
import type { CampusCreateReq, CampusEntity, CampusQueryParams, CampusUpdateReq } from '../types/system';

// 校区管理接口，和后端 /api/teaching/campuses 保持一一对应。
export function getCampusPage(params: CampusQueryParams) {
  return request.get<never, PageResult<CampusEntity>>('/api/teaching/campuses', { params });
}

export function getCampusDetail(id: number) {
  return request.get<never, CampusEntity>(`/api/teaching/campuses/${id}`);
}

export function createCampus(data: CampusCreateReq) {
  return request.post<never, number>('/api/teaching/campuses', data);
}

export function updateCampus(id: number, data: CampusUpdateReq) {
  return request.put(`/api/teaching/campuses/${id}`, data);
}

export function deleteCampus(id: number) {
  return request.delete(`/api/teaching/campuses/${id}`);
}
