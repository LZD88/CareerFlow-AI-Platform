export interface LoginReq {
  username: string;
  password: string;
}

export interface LoginRes {
  token: string;
  expiresIn: number;
  userId: number;
  username: string;
  nickname: string;
  unionId?: string;
  signSecret: string;
  signSecretExpiresIn: number;
  roleCodes: string[];
  permissionCodes: string[];
}

export interface LoginUser {
  userId: number;
  username: string;
  nickname: string;
  unionId?: string;
  signSecret: string;
  signSecretExpiresIn: number;
  roleCodes: string[];
  permissionCodes: string[];
}

export interface AuthProfileRes {
  userId: number;
  username: string;
  nickname: string;
  unionId?: string;
  feishuBound: boolean;
  roleCodes: string[];
  permissionCodes: string[];
}

export interface ChangePasswordReq {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}
