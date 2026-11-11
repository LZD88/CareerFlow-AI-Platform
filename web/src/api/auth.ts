import request from '../utils/request';
import type { AuthProfileRes, ChangePasswordReq, LoginReq, LoginRes } from '../types/auth';

export function login(data: LoginReq) {
  return request.post<never, LoginRes>('/api/auth/login', data);
}

export function feishuLogin(code: string) {
  return request.post<never, LoginRes>('/api/auth/feishu-login', { code });
}

export function getAuthProfile() {
  return request.get<never, AuthProfileRes>('/api/auth/profile');
}

export function bindFeishu(code: string) {
  return request.post('/api/auth/feishu/bind', { code });
}

export function unbindFeishu() {
  return request.delete('/api/auth/feishu/bind');
}

export function changePassword(data: ChangePasswordReq) {
  return request.post('/api/auth/change-password', data);
}
