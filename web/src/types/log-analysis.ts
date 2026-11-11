export interface LogSearchReq {
  query?: string;
  startTime: string;
  endTime: string;
}

export interface LogDiagnoseReq {
  guid: string;
}

export interface LogRecordRes {
  time?: string;
  content?: string;
}

export interface LogSearchRes {
  topicId: string;
  query: string;
  startTime: string;
  endTime: string;
  limit: number;
  isAnalysis?: boolean;
  count?: number;
  hitCount?: number;
  resultStatus?: string;
  logs: LogRecordRes[];
  rawResponse?: string;
}

export interface LogDiagnosisRes {
  guid: string;
  query: string;
  startTime: number;
  endTime: number;
  requestUri?: string;
  httpMethod?: string;
  controller?: string;
  requestArgs?: string;
  exceptionClass?: string;
  exceptionMessage?: string;
  topStackFrame?: string;
  simulatedReason?: string;
  possibleReason?: string;
  rootCause?: string;
  confidence?: number;
  evidenceLogs?: string[];
  relatedFiles?: string[];
  nextActions?: string[];
  needMoreInfo?: boolean;
  missingInfo?: string[];
  manualCheckItems?: string[];
  analysisSource?: string;
  analysisSummary?: string;
  relatedCode?: string[];
  suggestions?: string[];
  matchedLogCount?: number;
  rawLogs: string[];
}

export interface LogDiagnosisStreamEvent {
  stage: string;
  status: string;
  message: string;
  data?: unknown;
  timestamp?: number;
}
