import request from '../utils/request';
import type { PageResult, QueryParams } from '../types/common';
import type { CourseCreateReq, CourseEntity, CourseUpdateReq } from '../types/system';

// 课程管理接口，和后端 /api/teaching/courses 保持一一对应。
export function getCoursePage(params: QueryParams) {
  return request.get<never, PageResult<CourseEntity>>('/api/teaching/courses', { params });
}

export function getCourseDetail(id: number) {
  return request.get<never, CourseEntity>(`/api/teaching/courses/${id}`);
}

export function createCourse(data: CourseCreateReq) {
  return request.post<never, number>('/api/teaching/courses', data);
}

export function updateCourse(id: number, data: CourseUpdateReq) {
  return request.put(`/api/teaching/courses/${id}`, data);
}

export function deleteCourse(id: number) {
  return request.delete(`/api/teaching/courses/${id}`);
}
