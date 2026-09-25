// 화면 공통 표시 형식 — 화면 목록 3장 규칙 8
import dayjs from 'dayjs'

const numberFormat = new Intl.NumberFormat('ko-KR')

export function formatNumber(value: number | null | undefined): string {
  return value == null ? '-' : numberFormat.format(value)
}

/** 29000 → 29,000원 */
export function formatMoney(value: number | null | undefined): string {
  return value == null ? '-' : `${numberFormat.format(value)}원`
}

/** YYYY-MM-DD */
export function formatDate(value: string | null | undefined): string {
  return value ? dayjs(value).format('YYYY-MM-DD') : '-'
}

/** YYYY-MM-DD HH:mm */
export function formatDateTime(value: string | null | undefined): string {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}

/** 1234567890 → 123-45-67890 */
export function formatBusinessNumber(value: string | null | undefined): string {
  if (!value) return '-'
  return value.length === 10
    ? `${value.slice(0, 3)}-${value.slice(3, 5)}-${value.slice(5)}`
    : value
}

/** +5, −3 처럼 부호를 붙인다. */
export function formatSignedQuantity(value: number): string {
  return value > 0
    ? `+${formatNumber(value)}`
    : `−${formatNumber(Math.abs(value))}`
}
