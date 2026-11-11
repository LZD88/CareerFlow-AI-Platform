import { ACCESS_TOKEN_KEY, LOGIN_USER_KEY } from '../constants/auth';
import type { LoginRes, LoginUser } from '../types/auth';

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY) ?? '';
}

export function setLoginSession(data: LoginRes) {
  localStorage.setItem(ACCESS_TOKEN_KEY, data.token);
  const user: LoginUser = {
    userId: data.userId,
    username: data.username,
    nickname: data.nickname,
    unionId: data.unionId,
    signSecret: data.signSecret,
    signSecretExpiresIn: data.signSecretExpiresIn,
    roleCodes: data.roleCodes,
    permissionCodes: data.permissionCodes
  };
  localStorage.setItem(LOGIN_USER_KEY, JSON.stringify(user));
}

export function getLoginUser(): LoginUser | null {
  const raw = localStorage.getItem(LOGIN_USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as LoginUser;
  } catch {
    clearLoginSession();
    return null;
  }
}

export function clearLoginSession() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(LOGIN_USER_KEY);
}

export function isLoggedIn() {
  return Boolean(getAccessToken());
}
