import type { InternalAxiosRequestConfig } from 'axios';
import { getLoginUser } from './auth';

type SignableRequestConfig = Partial<Pick<InternalAxiosRequestConfig, 'method' | 'baseURL' | 'url' | 'params' | 'data' | 'headers'>>;

const apiSignPathPrefix = normalizePathPrefix(import.meta.env.VITE_API_SIGN_PATH_PREFIX);

export async function appendSignHeaders(config: InternalAxiosRequestConfig) {
  const signedHeaders = await buildSignedHeaders(config);
  Object.entries(signedHeaders).forEach(([key, value]) => {
    if (typeof config.headers.set === 'function') {
      config.headers.set(key, value);
    } else {
      config.headers[key] = value;
    }
  });
  return config;
}

export async function buildSignedHeaders(config: SignableRequestConfig): Promise<Record<string, string>> {
  const loginUser = getLoginUser();
  if (!loginUser?.signSecret) {
    return {};
  }

  const timestamp = Date.now().toString();
  const nonce = buildNonce();
  const method = (config.method ?? 'GET').toUpperCase();
  const path = buildRequestPath(config);
  const payload = buildPayload(config, method);
  const signText = [timestamp, nonce, method, path, payload].join('\n');
  const sign = await hmacSha256Hex(signText, loginUser.signSecret);
  return {
    'X-Timestamp': timestamp,
    'X-Nonce': nonce,
    'X-Sign': sign
  };
}

export function serializeParams(params: unknown) {
  const pairs: Array<[string, string]> = [];
  appendParams(pairs, params);
  return serializePairs(pairs);
}

function serializePairs(pairs: Array<[string, string]>) {
  return pairs
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .sort((a, b) => a.localeCompare(b))
    .join('&');
}

function buildRequestPath(config: SignableRequestConfig) {
  const baseURL = resolveRequestBaseURL(config.baseURL);
  const rawUrl = config.url ?? '';
  const url = new URL(rawUrl, baseURL);
  return `${apiSignPathPrefix}${url.pathname}`;
}

function buildPayload(config: SignableRequestConfig, method: string) {
  if (method === 'GET') {
    return canonicalizeQuery(config);
  }
  // 文件上传使用 multipart/form-data，浏览器会自动拼接 boundary，
  // 直接对 FormData 原始内容签名会和服务端读取结果不稳定，因此统一视为空 payload。
  if (isMultipartPayload(config.data, config.headers?.['Content-Type'])) {
    return '';
  }
  return normalizePayload(config.data);
}

function canonicalizeQuery(config: SignableRequestConfig) {
  const baseURL = resolveRequestBaseURL(config.baseURL);
  const rawUrl = config.url ?? '';
  const url = new URL(rawUrl, baseURL);
  const pairs: Array<[string, string]> = [];

  url.searchParams.forEach((value, key) => {
    pairs.push([key, value]);
  });
  appendParams(pairs, config.params);

  return serializePairs(pairs);
}

function resolveRequestBaseURL(baseURL?: string) {
  if (baseURL?.trim()) {
    return baseURL;
  }
  return window.location.origin;
}

function normalizePathPrefix(prefix?: string) {
  const trimmed = prefix?.trim();
  if (!trimmed) {
    return '';
  }
  const normalized = trimmed.startsWith('/') ? trimmed : `/${trimmed}`;
  return normalized.endsWith('/') ? normalized.slice(0, -1) : normalized;
}

function appendParams(pairs: Array<[string, string]>, value: unknown, prefix?: string) {
  if (value === undefined || value === null) {
    return;
  }

  if (Array.isArray(value)) {
    value.forEach((item) => appendParams(pairs, item, prefix));
    return;
  }

  if (value instanceof Date) {
    pairs.push([prefix ?? '', value.toISOString()]);
    return;
  }

  if (typeof value === 'object') {
    Object.entries(value as Record<string, unknown>).forEach(([key, childValue]) => {
      appendParams(pairs, childValue, prefix ? `${prefix}.${key}` : key);
    });
    return;
  }

  pairs.push([prefix ?? '', String(value)]);
}

function normalizePayload(value: unknown): string {
  if (value === undefined || value === null || value === '') {
    return '';
  }

  const raw = typeof value === 'string' ? value : JSON.stringify(value);
  try {
    const parsed = JSON.parse(raw) as unknown;
    return stableStringify(parsed);
  } catch {
    return raw.trim();
  }
}

function stableStringify(value: unknown): string {
  if (Array.isArray(value)) {
    return `[${value.map((item) => stableStringify(item)).join(',')}]`;
  }

  if (value && typeof value === 'object') {
    const entries = Object.entries(value as Record<string, unknown>).sort(([left], [right]) => left.localeCompare(right));
    return `{${entries.map(([key, child]) => `${JSON.stringify(key)}:${stableStringify(child)}`).join(',')}}`;
  }

  return JSON.stringify(value);
}

function buildNonce() {
  return `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 10)}`;
}

function isMultipartPayload(data: unknown, contentType?: unknown) {
  if (typeof FormData !== 'undefined' && data instanceof FormData) {
    return true;
  }
  return typeof contentType === 'string' && contentType.toLowerCase().startsWith('multipart/form-data');
}

async function hmacSha256(message: string, secret: string) {
  const encoder = new TextEncoder();
  const key = await crypto.subtle.importKey(
    'raw',
    encoder.encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  );
  return crypto.subtle.sign('HMAC', key, encoder.encode(message));
}

export async function hmacSha256Hex(message: string, secret: string) {
  const buffer = await hmacSha256(message, secret);
  return Array.from(new Uint8Array(buffer))
    .map((item) => item.toString(16).padStart(2, '0'))
    .join('');
}
