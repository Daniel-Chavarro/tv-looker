export interface ApiError {
  message: string;
  code?: string;
  statusCode: number;
}

// Generic interface for paginated API responses (future use)
export interface PaginatedResponse<T> {
  data: T[];
  total: number;
  page: number;
  pageSize: number;
}
