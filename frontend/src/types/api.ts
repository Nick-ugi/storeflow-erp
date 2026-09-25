// API 공통 응답 형식 — docs/api/01-api-common.md 4장

export interface ApiResponse<T> {
  success: true
  data: T
}

export interface FieldErrorDetail {
  field: string
  message: string
}

export interface ApiErrorBody {
  success: false
  error: {
    code: string
    message: string
    fieldErrors: FieldErrorDetail[]
  }
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface IdResponse {
  id: number
}

export interface StatusResponse<S> {
  id: number
  status: S
}

/** 쿼리 파라미터 값 (없거나 빈 값은 요청에서 뺀다) */
export type QueryParams = Record<
  string,
  string | number | boolean | null | undefined
>
