export type PaginatedResponse<T> = {
  content: T[];
  actualPage: number;
  totalPages: number;
  totalItems: number;
  isLast: boolean;
};
