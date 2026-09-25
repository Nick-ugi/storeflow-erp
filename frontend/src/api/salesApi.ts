// API-SALE-001 ~ 004
import { apiGet, apiPost } from '@/api/client'
import type { PageResponse, QueryParams } from '@/types/api'
import type {
  Sale,
  SaleCancelResponse,
  SaleCreateRequest,
  SaleCreateResponse,
  SaleDetail,
} from '@/types/domain'

export const salesApi = {
  search: (params: QueryParams) => apiGet<PageResponse<Sale>>('/sales', params),
  get: (id: number) => apiGet<SaleDetail>(`/sales/${id}`),
  create: (request: SaleCreateRequest) =>
    apiPost<SaleCreateResponse>('/sales', request),
  cancel: (id: number) => apiPost<SaleCancelResponse>(`/sales/${id}/cancel`),
}
