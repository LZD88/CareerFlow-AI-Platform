import request from '../utils/request';
import type { ClassDutySaveReq, DutyManagementDayRes, GlobalDutySaveReq } from '../types/system';

export function getDutyManagementDay(dutyDate: string) {
  return request.get<never, DutyManagementDayRes>('/api/teaching/duties', {
    params: { dutyDate }
  });
}

export function saveClassDuty(classId: number, data: ClassDutySaveReq) {
  return request.post(`/api/teaching/duties/classes/${classId}`, data);
}

export function saveGlobalDuty(data: GlobalDutySaveReq) {
  return request.post('/api/teaching/duties/global', data);
}
