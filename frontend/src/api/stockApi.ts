// API-STOCK-001 ~ 005
import { apiGet, apiPost, apiPut } from '@/api/client'
import type { PageResponse, QueryParams } from '@/types/api'
import type {
  SafetyStockResponse,
  Stock,
  StockAdjustmentRequest,
  StockAdjustmentResponse,
  StockDetail,
  StockHistory,
} from '@/types/domain'

export const stockApi = {
  search: (params: QueryParams) =>
    apiGet<PageResponse<Stock>>('/stocks', params),
  get: (productId: number, storeId?: number | null) =>
    apiGet<StockDetail>(`/stocks/${productId}`, { storeId }),
  histories: (params: QueryParams) =>
    apiGet<PageResponse<StockHistory>>('/stocks/histories', params),
  adjust: (request: StockAdjustmentRequest) =>
    apiPost<StockAdjustmentResponse>('/stocks/adjustments', request),
  updateSafetyStock: (
    productId: number,
    storeId: number | null,
    safetyStock: number,
  ) =>
    apiPut<SafetyStockResponse>(
      `/stocks/${productId}/safety-stock`,
      { safetyStock },
      { storeId },
    ),
}
