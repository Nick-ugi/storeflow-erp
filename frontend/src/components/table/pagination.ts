import type { TablePaginationConfig } from 'antd'
import type { PageResponse } from '@/types/api'

/** 서버 페이지(0부터) ↔ antd 페이지(1부터) 변환, 총 건수 표시 */
export function tablePagination<T>(
  data: PageResponse<T> | undefined,
  onChange: (page: number) => void,
): TablePaginationConfig {
  return {
    current: (data?.page ?? 0) + 1,
    pageSize: data?.size ?? 20,
    total: data?.totalElements ?? 0,
    showSizeChanger: false,
    showTotal: (total) => `총 ${total.toLocaleString('ko-KR')}건`,
    onChange: (page) => onChange(page - 1),
  }
}
