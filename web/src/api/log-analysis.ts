import request, { apiBaseURL } from '../utils/request';
import type { LogDiagnoseReq, LogDiagnosisRes, LogDiagnosisStreamEvent, LogSearchReq, LogSearchRes } from '../types/log-analysis';
import { getAccessToken } from '../utils/auth';
import { buildSignedHeaders } from '../utils/signature';

export function searchLogs(data: LogSearchReq) {
  return request.post<never, LogSearchRes>('/api/log-analysis/search', data);
}

export function diagnoseLogs(data: LogDiagnoseReq) {
  return request.post<never, LogDiagnosisRes>('/api/log-analysis/diagnose', data, {
    timeout: 70000
  });
}

export async function diagnoseLogsStream(
  data: LogDiagnoseReq,
  handlers: {
    onEvent: (event: LogDiagnosisStreamEvent) => void;
    onDone?: () => void;
  }
) {
  const url = `${apiBaseURL}/api/log-analysis/diagnose/stream`;
  const signedHeaders = await buildSignedHeaders({
    method: 'POST',
    baseURL: apiBaseURL,
    url: '/api/log-analysis/diagnose/stream',
    data
  });

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream',
    ...signedHeaders
  };
  const token = getAccessToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(url, {
    method: 'POST',
    headers,
    body: JSON.stringify(data)
  });

  if (!response.ok || !response.body) {
    throw new Error(`流式诊断请求失败: ${response.status}`);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';

  while (true) {
    const { value, done } = await reader.read();
    if (done) {
      break;
    }
    buffer += decoder.decode(value, { stream: true });

    let boundaryIndex = buffer.indexOf('\n\n');
    while (boundaryIndex >= 0) {
      const rawEvent = buffer.slice(0, boundaryIndex);
      buffer = buffer.slice(boundaryIndex + 2);
      const payload = parseSseEvent(rawEvent);
      if (payload) {
        handlers.onEvent(payload);
      }
      boundaryIndex = buffer.indexOf('\n\n');
    }
  }

  handlers.onDone?.();
}

function parseSseEvent(rawEvent: string): LogDiagnosisStreamEvent | null {
  const dataLines = rawEvent
    .split('\n')
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).trim());
  if (!dataLines.length) {
    return null;
  }
  return JSON.parse(dataLines.join('\n')) as LogDiagnosisStreamEvent;
}
