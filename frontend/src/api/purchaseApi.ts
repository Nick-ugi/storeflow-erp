// API-PO-001 ~ 007
import { apiGet, apiPost, apiPut } from '@/api/client'
import type {
  IdResponse,
  PageResponse,
  QueryParams,
  StatusResponse,
} from '@/types/api'
import type { PurchaseOrderStatus } from '@/types/code'
import type {
  PurchaseOrder,
  PurchaseOrderCreateResponse,
  PurchaseOrderDetail,
  PurchaseOrderRequest,
} from '@/types/domain'

export type PurchaseOrderAction = 'approve' | 'receive' | 'cancel'

export const purchaseApi = {
  search: (params: QueryParams) =>
    apiGet<PageResponse<PurchaseOrder>>('/purchase-orders', params),
  get: (id: number) => apiGet<PurchaseOrderDetail>(`/purchase-orders/${id}`),
  create: (request: PurchaseOrderRequest) =>
    apiPost<PurchaseOrderCreateResponse>('/purchase-orders', request),
  update: (id: number, request: PurchaseOrderRequest) =>
    apiPut<IdResponse>(`/purchase-orders/${id}`, request),
  changeStatus: (id: number, action: PurchaseOrderAction) =>
    apiPost<StatusResponse<PurchaseOrderStatus>>(
      `/purchase-orders/${id}/${action}`,
    ),
}
