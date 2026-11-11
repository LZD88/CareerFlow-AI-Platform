import request from '../utils/request';
import type { PageResult } from '../types/common';
import type { StudentDailyMomentAdminItem, StudentDailyMomentAdminQueryParams } from '../types/system';

export function getStudentDailyMomentPage(params: StudentDailyMomentAdminQueryParams) {
  return request.get<never, PageResult<StudentDailyMomentAdminItem>>('/api/students/daily-moments', { params });
}
