import request from '../utils/request';
import type { TeacherScheduleCalendarRes } from '../types/system';

export interface TeacherScheduleCalendarParams {
  startDate: string;
  endDate: string;
  keyword?: string;
  teacherId?: number;
}

export function getTeacherScheduleCalendar(params: TeacherScheduleCalendarParams) {
  return request.get<never, TeacherScheduleCalendarRes>('/api/teaching/schedules/teachers/calendar', {
    params
  });
}
