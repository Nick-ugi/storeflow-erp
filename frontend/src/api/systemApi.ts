// API-STORE-001 ~ 007, API-USER-001 ~ 007
import { apiGet, apiPost, apiPut } from '@/api/client'
import type {
  IdResponse,
  PageResponse,
  QueryParams,
  StatusResponse,
} from '@/types/api'
import type { ActiveStatus } from '@/types/code'
import type {
  Store,
  StoreDetail,
  StoreOption,
  StoreRequest,
  User,
  UserDetail,
  UserRequest,
} from '@/types/domain'

export const storeApi = {
  search: (params: QueryParams) =>
    apiGet<PageResponse<Store>>('/stores', params),
  options: () => apiGet<StoreOption[]>('/stores/options'),
  get: (id: number) => apiGet<StoreDetail>(`/stores/${id}`),
  create: (request: StoreRequest) => apiPost<IdResponse>('/stores', request),
  update: (id: number, request: StoreRequest) =>
    apiPut<IdResponse>(`/stores/${id}`, request),
  setActive: (id: number, active: boolean) =>
    apiPost<StatusResponse<ActiveStatus>>(
      `/stores/${id}/${active ? 'activate' : 'deactivate'}`,
    ),
}

export const userApi = {
  search: (params: QueryParams) => apiGet<PageResponse<User>>('/users', params),
  get: (id: number) => apiGet<UserDetail>(`/users/${id}`),
  create: (request: UserRequest) => apiPost<IdResponse>('/users', request),
  update: (id: number, request: UserRequest) =>
    apiPut<IdResponse>(`/users/${id}`, request),
  setActive: (id: number, active: boolean) =>
    apiPost<StatusResponse<ActiveStatus>>(
      `/users/${id}/${active ? 'activate' : 'deactivate'}`,
    ),
  resetPassword: (id: number, newPassword: string) =>
    apiPost<null>(`/users/${id}/reset-password`, { newPassword }),
}
