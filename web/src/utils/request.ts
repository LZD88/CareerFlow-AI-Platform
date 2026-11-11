import axios from 'axios';
import { message } from 'antd';
import type { ApiResponse } from '../types/common';
import { clearLoginSession, getAccessToken } from './auth';
import { appendSignHeaders, serializeParams } from './signature';

declare module 'axios' {
  export interface AxiosRequestConfig {
    silent?: boolean;
  }
}

function resolveApiBaseURL() {
  const envBaseURL = import.meta.env.VITE_API_BASE_URL?.trim();
  if (envBaseURL) {
    return envBaseURL;
  }
  return import.meta.env.DEV ? 'http://127.0.0.1:8081/yq-admin' : '';
}

const apiBaseURL = resolveApiBaseURL();

const request = axios.create({
  // 开发环境默认直连本地后端，生产环境默认走同域名反向代理。
  baseURL: apiBaseURL,
  timeout: 10000,
  paramsSerializer: {
    serialize: serializeParams
  }
});

request.interceptors.request.use(async (config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
    await appendSignHeaders(config);
  }
  return config;
});

request.interceptors.response.use(
  ((response: any) => {
    const data = response.data as ApiResponse<unknown>;
    if (data.code !== 200) {
      if (data.code === 401) {
        clearLoginSession();
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
      }
      if (!response.config.silent) message.error(data.message || '请求失败');
      return Promise.reject(new Error(data.message || 'Request Error'));
    }
    return data.data;
  }) as any,
  (error) => {
    if (error?.response?.status === 401) {
      clearLoginSession();
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    if (!error.config?.silent) message.error(error?.response?.data?.message || error.message || '网络异常');
    return Promise.reject(error);
  }
);

export default request;

export { apiBaseURL };
