export interface DataAnalysisAnalyzeReq {
  question: string;
  maxRows?: number;
}

export interface DataAnalysisChartSuggestion {
  chartType?: string;
  xField?: string;
  yField?: string;
  seriesField?: string;
  reason?: string;
}

export interface DataAnalysisAnalyzeRes {
  question?: string;
  candidateTables?: string[];
  sql?: string;
  reason?: string;
  needsClarification?: boolean;
  clarificationQuestions?: string[];
  knowledgeMissing?: boolean;
  knowledgeMissingItems?: string[];
  columns?: string[];
  rows?: Record<string, unknown>[];
  rowCount?: number;
  truncated?: boolean;
  analysis?: string;
  insights?: string[];
  followUpQuestions?: string[];
  chartSuggestion?: DataAnalysisChartSuggestion;
  assumptions?: string[];
  warnings?: string[];
  missingTables?: string[];
}
