import axios, { type AxiosError } from 'axios'
import { useAuthStore } from '@/stores/authStore'
import type {
  ApiErrorBody,
  ApiResponse,
  FieldErrorDetail,
  QueryParams,
} from '@/types/api'

/** API 오류 — 공통 규칙 4.3의 error를 담는다. */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors: FieldErrorDetail[]

  constructor(
    status: number,
    code: string,
    message: string,
    fieldErrors: FieldErrorDetail[] = [],
  ) {
    super(message)
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }
}

const http = axios.create({ baseURL: '/api/v1', timeout: 15_000 })

http.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorBody>) => {
    const status = error.response?.status ?? 0
    const body = error.response?.data
    const apiError = body?.error
      ? new ApiError(
          status,
          body.error.code,
          body.error.message,
          body.error.fieldErrors ?? [],
        )
      : new ApiError(
          status,
          'NETWORK_ERROR',
          '서버에 연결할 수 없습니다. 잠시 후 다시 시도하세요.',
        )

    // 토큰 만료 · 비활성화: 로그인 화면으로 보내고, 로그인 후 원래 화면으로 돌아온다. (API 공통 규칙 6장)
    if (apiError.code === 'UNAUTHORIZED' && useAuthStore.getState().token) {
      useAuthStore.getState().logout()
      const redirect = encodeURIComponent(
        window.location.pathname + window.location.search,
      )
      window.location.assign(`/login?expired=1&redirect=${redirect}`)
    }
    return Promise.reject(apiError)
  },
)

/** 값이 없는 파라미터는 보내지 않는다. */
function cleanParams(params?: QueryParams) {
  if (!params) return undefined
  return Object.fromEntries(
    Object.entries(params).filter(
      ([, v]) => v !== undefined && v !== null && v !== '',
    ),
  )
}

export async function apiGet<T>(url: string, params?: QueryParams): Promise<T> {
  const response = await http.get<ApiResponse<T>>(url, {
    params: cleanParams(params),
  })
  return response.data.data
}

export async function apiPost<T>(
  url: string,
  body?: unknown,
  params?: QueryParams,
): Promise<T> {
  const response = await http.post<ApiResponse<T>>(url, body, {
    params: cleanParams(params),
  })
  return response.data.data
}

export async function apiPut<T>(
  url: string,
  body?: unknown,
  params?: QueryParams,
): Promise<T> {
  const response = await http.put<ApiResponse<T>>(url, body, {
    params: cleanParams(params),
  })
  return response.data.data
}
