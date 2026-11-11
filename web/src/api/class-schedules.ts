import request from '../utils/request';
import type {
  AssignStageTeacherReq,
  ClassScheduleArrangeReq,
  ClassScheduleCalendarRes,
  ClassScheduleChangeContentReq,
  ClassScheduleChangeTeacherReq,
  ClassScheduleDeleteReq,
  ClassScheduleGenerateReq,
  ClassStageTeacher,
  StageTeacherOption
} from '../types/system';

export function generateClassSchedule(classId: number, data: ClassScheduleGenerateReq) {
  return request.post(`/api/teaching/schedules/classes/${classId}/generate`, data);
}

export function arrangeClassSchedule(classId: number, data: ClassScheduleArrangeReq) {
  return request.post(`/api/teaching/schedules/classes/${classId}/arrange`, data);
}

export function getArrangeTeacherOptions(classId: number, scheduleDate: string) {
  return request.get<never, StageTeacherOption[]>(`/api/teaching/schedules/classes/${classId}/arrange`, {
    params: { scheduleDate }
  });
}

export function deleteClassSchedule(classId: number, data: ClassScheduleDeleteReq) {
  return request.delete(`/api/teaching/schedules/classes/${classId}/arrange`, { data });
}

export function changeClassScheduleTeacher(classId: number, data: ClassScheduleChangeTeacherReq) {
  return request.put(`/api/teaching/schedules/classes/${classId}/arrange/teacher`, data);
}

export function changeClassScheduleContent(classId: number, data: ClassScheduleChangeContentReq) {
  return request.put(`/api/teaching/schedules/classes/${classId}/arrange/content`, data);
}

export function getClassStageTeachers(classId: number) {
  return request.get<never, ClassStageTeacher[]>(`/api/teaching/schedules/classes/${classId}/stage-teachers`);
}

export function assignClassStageTeachers(classId: number, data: AssignStageTeacherReq) {
  return request.put(`/api/teaching/schedules/classes/${classId}/stage-teachers`, data);
}

export function getClassScheduleCalendar(classId: number, year?: number, month?: number) {
  return request.get<never, ClassScheduleCalendarRes>(`/api/teaching/schedules/classes/${classId}/calendar`, {
    params: { year, month }
  });
}
