import { Alert } from 'antd'
import { ApiError } from '@/api/client'
import { ErrorPage } from '@/pages/ErrorPage'

/** 상세 조회 실패: 없는 데이터(404) · 권한 없음(403)은 오류 안내 화면, 그 외는 메시지 (SCR-COM-002) */
export function QueryError({ error }: { error: unknown }) {
  if (
    error instanceof ApiError &&
    (error.status === 403 || error.status === 404)
  ) {
    return <ErrorPage status={error.status === 403 ? 403 : 404} />
  }
  const message =
    error instanceof ApiError ? error.message : '데이터를 불러오지 못했습니다.'
  return <Alert type="error" showIcon title={message} />
}
