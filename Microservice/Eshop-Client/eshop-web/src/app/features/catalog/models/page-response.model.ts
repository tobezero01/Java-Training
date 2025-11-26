export interface PageResponse<T> {
  page: number;      // trang 1-based
  size: number;      // kích thước trang
  total: number;     // tổng số bản ghi
  totalPages: number;
  content: T[];
}
