export type ApiError = {
  message: string;
  code?: string;
  statusCode: number;
}

// Generic type for paginated API responses (future use)
export type PaginatedResponse<T> = {
  data: T[];
  total: number;
  page: number;
  pageSize: number;
}
