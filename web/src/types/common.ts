export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface PageResult<T> {
  total: number;
  pageNum: number;
  pageSize: number;
  records: T[];
}

export interface QueryParams {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: number;
  roleCode?: string;
}

export interface OptionItem {
  label: string;
  value: number;
}
