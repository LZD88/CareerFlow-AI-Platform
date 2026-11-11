import request from '../utils/request';
import type { PageResult } from '../types/common';
import type { ClassCreateReq, ClassEntity, ClassQueryParams, ClassUpdateReq } from '../types/system';

export function getClassPage(params: ClassQueryParams) {
  return request.get<never, PageResult<ClassEntity>>('/api/teaching/classes', { params });
}

export function getClassDetail(id: number) {
  return request.get<never, ClassEntity>(`/api/teaching/classes/${id}`);
}

export function createClass(data: ClassCreateReq) {
  return request.post<never, number>('/api/teaching/classes', data);
}

export function updateClass(id: number, data: ClassUpdateReq) {
  return request.put(`/api/teaching/classes/${id}`, data);
}

export function deleteClass(id: number) {
  return request.delete(`/api/teaching/classes/${id}`);
}
