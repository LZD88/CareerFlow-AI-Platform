import request from '../utils/request';
import type { CourseDetailCreateReq, CourseDetailEntity, CourseDetailUpdateReq } from '../types/system';

export function getCourseDetailList(courseId: number) {
  return request.get<never, CourseDetailEntity[]>(`/api/teaching/courses/${courseId}/details`);
}

export function getCourseDetailItem(courseId: number, id: number) {
  return request.get<never, CourseDetailEntity>(`/api/teaching/courses/${courseId}/details/${id}`);
}

export function createCourseDetail(courseId: number, data: CourseDetailCreateReq) {
  return request.post<never, number>(`/api/teaching/courses/${courseId}/details`, data);
}

export function updateCourseDetail(courseId: number, id: number, data: CourseDetailUpdateReq) {
  return request.put(`/api/teaching/courses/${courseId}/details/${id}`, data);
}

export function deleteCourseDetail(courseId: number, id: number) {
  return request.delete(`/api/teaching/courses/${courseId}/details/${id}`);
}

export function deleteAllCourseDetails(courseId: number) {
  return request.delete(`/api/teaching/courses/${courseId}/details`);
}

export function importCourseDetails(courseId: number, file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return request.post<never, number>(`/api/teaching/courses/${courseId}/details/import`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}
