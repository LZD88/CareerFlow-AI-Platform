const FEISHU_RETURN_TO_KEY = 'yq_feishu_return_to';
const FEISHU_STATUS_KEY = 'yq_feishu_status';
const FEISHU_STATE_KEY = 'yq_feishu_state';
const FEISHU_ACTION_KEY = 'yq_feishu_action';
const FEISHU_AUTHORIZE_URL = 'https://accounts.feishu.cn/open-apis/authen/v1/authorize';

function getFeishuAppId() {
  return import.meta.env.VITE_FEISHU_APP_ID?.trim() || '';
}

function resolveFeishuRedirectUri() {
  if (import.meta.env.VITE_FEISHU_REDIRECT_URI?.trim()) {
    return import.meta.env.VITE_FEISHU_REDIRECT_URI.trim();
  }
  if (typeof window === 'undefined') {
    return '';
  }
  return `${window.location.origin}/feishu/callback`;
}

function getFeishuAuthUrlTemplate() {
  return import.meta.env.VITE_FEISHU_AUTH_URL?.trim() ?? '';
}

export function canLaunchFeishuAuth() {
  return Boolean(getFeishuAppId() && resolveFeishuRedirectUri());
}

export function buildFeishuAuthorizeUrl(action: 'bind' | 'login' = 'bind') {
  const appId = getFeishuAppId();
  const redirectUri = resolveFeishuRedirectUri();
  const state = createFeishuState(action);
  const template = getFeishuAuthUrlTemplate();

  if (!appId) {
    return '';
  }

  saveFeishuState(state);
  saveFeishuAction(action);

  if (template) {
    return template
      .replace('{appId}', encodeURIComponent(appId))
      .replace('{redirectUri}', encodeURIComponent(redirectUri))
      .replace('{state}', encodeURIComponent(state));
  }

  const params = new URLSearchParams({
    client_id: appId,
    redirect_uri: redirectUri,
    response_type: 'code',
    state
  });

  return `${FEISHU_AUTHORIZE_URL}?${params.toString()}`;
}

function createFeishuState(action: 'bind' | 'login') {
  return `feishu_${action}_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
}

function saveFeishuState(state: string) {
  sessionStorage.setItem(FEISHU_STATE_KEY, state);
}

function saveFeishuAction(action: 'bind' | 'login') {
  sessionStorage.setItem(FEISHU_ACTION_KEY, action);
}

export function consumeFeishuAction() {
  const value = sessionStorage.getItem(FEISHU_ACTION_KEY) as 'bind' | 'login' | null;
  sessionStorage.removeItem(FEISHU_ACTION_KEY);
  return value ?? 'bind';
}

export function consumeFeishuBindState() {
  const value = sessionStorage.getItem(FEISHU_STATE_KEY) || '';
  sessionStorage.removeItem(FEISHU_STATE_KEY);
  return value;
}

export function peekFeishuBindState() {
  return sessionStorage.getItem(FEISHU_STATE_KEY) || '';
}

export function clearFeishuBindState() {
  sessionStorage.removeItem(FEISHU_STATE_KEY);
}

export function getFeishuRedirectUri() {
  return resolveFeishuRedirectUri();
}

export function getFeishuAppIdForDisplay() {
  return getFeishuAppId();
}

export function getFeishuAuthorizeBaseUrl() {
  const template = getFeishuAuthUrlTemplate();
  return template
    ? template
    : FEISHU_AUTHORIZE_URL;
}

export function saveFeishuBindReturnTo(pathname: string) {
  sessionStorage.setItem(FEISHU_RETURN_TO_KEY, pathname);
}

export function consumeFeishuBindReturnTo() {
  const value = sessionStorage.getItem(FEISHU_RETURN_TO_KEY) || '/';
  sessionStorage.removeItem(FEISHU_RETURN_TO_KEY);
  return value;
}

export function saveFeishuBindStatus(status: 'success' | 'error', message: string) {
  sessionStorage.setItem(FEISHU_STATUS_KEY, JSON.stringify({ status, message }));
}

export function consumeFeishuBindStatus(): { status: 'success' | 'error'; message: string } | null {
  const raw = sessionStorage.getItem(FEISHU_STATUS_KEY);
  if (!raw) {
    return null;
  }
  sessionStorage.removeItem(FEISHU_STATUS_KEY);
  try {
    return JSON.parse(raw) as { status: 'success' | 'error'; message: string };
  } catch {
    return null;
  }
}
