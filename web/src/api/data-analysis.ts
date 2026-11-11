import request from '../utils/request';
import type { DataAnalysisAnalyzeReq, DataAnalysisAnalyzeRes } from '../types/data-analysis';

export async function analyzeData(data: DataAnalysisAnalyzeReq) {
  const response = await request.post<never, DataAnalysisAnalyzeRes>('/api/data-analysis/analyze', data, {
    timeout: 240000
  });
  return normalizeAnalyzeResponse(response);
}

function normalizeAnalyzeResponse(response: DataAnalysisAnalyzeRes): DataAnalysisAnalyzeRes {
  return {
    ...response,
    candidateTables: parseStringList((response as any).candidateTables),
    needsClarification: Boolean((response as any).needsClarification),
    clarificationQuestions: parseStringList((response as any).clarificationQuestions),
    knowledgeMissing: Boolean((response as any).knowledgeMissing),
    knowledgeMissingItems: parseStringList((response as any).knowledgeMissingItems),
    columns: parseStringList((response as any).columns),
    rows: parseRows((response as any).rows),
    insights: parseStringList((response as any).insights),
    followUpQuestions: parseStringList((response as any).followUpQuestions),
    assumptions: parseStringList((response as any).assumptions),
    warnings: parseStringList((response as any).warnings),
    missingTables: parseStringList((response as any).missingTables)
  };
}

function parseStringList(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value.map((item) => String(item)).filter(Boolean);
  }
  if (typeof value !== 'string') {
    return [];
  }
  const text = value.trim();
  if (!text || text === '[]') {
    return [];
  }
  try {
    const parsed = JSON.parse(text);
    if (Array.isArray(parsed)) {
      return parsed.map((item) => String(item)).filter(Boolean);
    }
  } catch {
    // Java List#toString 不是合法 JSON，继续走下面的兼容解析。
  }
  if (text.startsWith('[') && text.endsWith(']')) {
    return text
      .slice(1, -1)
      .split(/,\s*/)
      .map((item) => item.trim())
      .filter(Boolean);
  }
  return [text];
}

function parseRows(value: unknown): Record<string, unknown>[] {
  if (Array.isArray(value)) {
    return value
      .filter((item) => item && typeof item === 'object')
      .map((item) => item as Record<string, unknown>);
  }
  if (typeof value !== 'string') {
    return [];
  }
  const text = value.trim();
  if (!text || text === '[]') {
    return [];
  }
  try {
    const parsed = JSON.parse(text);
    if (Array.isArray(parsed)) {
      return parseRows(parsed);
    }
  } catch {
    // 兼容 Java Map#toString: [{campus_name=杭州校区, total_payment_amount=0.01}]
  }
  return parseJavaMapListString(text);
}

function parseJavaMapListString(value: string): Record<string, unknown>[] {
  const text = value.startsWith('[') && value.endsWith(']') ? value.slice(1, -1).trim() : value;
  if (!text) {
    return [];
  }
  return splitJavaMapItems(text)
    .map(parseJavaMapString)
    .filter((row) => Object.keys(row).length > 0);
}

function splitJavaMapItems(value: string) {
  const items: string[] = [];
  let depth = 0;
  let start = 0;
  for (let index = 0; index < value.length; index += 1) {
    const char = value[index];
    if (char === '{') {
      depth += 1;
    } else if (char === '}') {
      depth -= 1;
      if (depth === 0) {
        items.push(value.slice(start, index + 1).trim());
        if (value[index + 1] === ',') {
          start = index + 2;
        }
      }
    }
  }
  return items.length ? items : [value];
}

function parseJavaMapString(value: string): Record<string, unknown> {
  const text = value.startsWith('{') && value.endsWith('}') ? value.slice(1, -1) : value;
  const row: Record<string, unknown> = {};
  text.split(/,\s*/).forEach((entry) => {
    const splitIndex = entry.indexOf('=');
    if (splitIndex <= 0) {
      return;
    }
    const key = entry.slice(0, splitIndex).trim();
    const rawValue = entry.slice(splitIndex + 1).trim();
    row[key] = parseScalar(rawValue);
  });
  return row;
}

function parseScalar(value: string): unknown {
  if (value === 'null') {
    return null;
  }
  if (value === 'true') {
    return true;
  }
  if (value === 'false') {
    return false;
  }
  const numberValue = Number(value);
  if (value !== '' && Number.isFinite(numberValue)) {
    return numberValue;
  }
  return value;
}
