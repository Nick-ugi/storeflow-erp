// API-PROD-001 ~ 006, API-CAT-001 ~ 003, API-SUPP-001 ~ 007
import { apiGet, apiPost, apiPut } from '@/api/client'
import type {
  IdResponse,
  PageResponse,
  QueryParams,
  StatusResponse,
} from '@/types/api'
import type { ActiveStatus } from '@/types/code'
import type {
  Category,
  CategoryRequest,
  Product,
  ProductDetail,
  ProductRequest,
  Supplier,
  SupplierDetail,
  SupplierOption,
  SupplierRequest,
} from '@/types/domain'

export const productApi = {
  search: (params: QueryParams) =>
    apiGet<PageResponse<Product>>('/products', params),
  get: (id: number) => apiGet<ProductDetail>(`/products/${id}`),
  create: (request: ProductRequest) =>
    apiPost<IdResponse>('/products', request),
  update: (id: number, request: ProductRequest) =>
    apiPut<IdResponse>(`/products/${id}`, request),
  setActive: (id: number, active: boolean) =>
    apiPost<StatusResponse<ActiveStatus>>(
      `/products/${id}/${active ? 'activate' : 'deactivate'}`,
    ),
}

export const categoryApi = {
  list: () => apiGet<Category[]>('/categories'),
  create: (request: CategoryRequest) =>
    apiPost<IdResponse>('/categories', request),
  update: (id: number, request: CategoryRequest) =>
    apiPut<IdResponse>(`/categories/${id}`, request),
}

export const supplierApi = {
  search: (params: QueryParams) =>
    apiGet<PageResponse<Supplier>>('/suppliers', params),
  options: () => apiGet<SupplierOption[]>('/suppliers/options'),
  get: (id: number) => apiGet<SupplierDetail>(`/suppliers/${id}`),
  create: (request: SupplierRequest) =>
    apiPost<IdResponse>('/suppliers', request),
  update: (id: number, request: SupplierRequest) =>
    apiPut<IdResponse>(`/suppliers/${id}`, request),
  setActive: (id: number, active: boolean) =>
    apiPost<StatusResponse<ActiveStatus>>(
      `/suppliers/${id}/${active ? 'activate' : 'deactivate'}`,
    ),
}
