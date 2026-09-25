// 코드 값 → 화면 표시 이름 · 색 (코드 정의서 2장)
import type {
  ActiveStatus,
  PurchaseOrderStatus,
  Role,
  SaleStatus,
  StockHistoryType,
} from '@/types/code'

interface CodeInfo {
  label: string
  color: string
}

export const ROLE_LABELS: Record<Role, string> = {
  ADMIN: '본사 관리자',
  MANAGER: '매장 관리자',
  USER: '매장 직원',
}

export const ACTIVE_STATUS: Record<ActiveStatus, CodeInfo> = {
  ACTIVE: { label: '사용', color: 'green' },
  INACTIVE: { label: '사용 중지', color: 'default' },
}

/** 사용자 상태는 '활성 / 비활성'으로 부른다. */
export const USER_STATUS: Record<ActiveStatus, CodeInfo> = {
  ACTIVE: { label: '활성', color: 'green' },
  INACTIVE: { label: '비활성', color: 'default' },
}

export const SALE_STATUS: Record<SaleStatus, CodeInfo> = {
  COMPLETED: { label: '완료', color: 'blue' },
  CANCELLED: { label: '취소', color: 'red' },
}

export const PURCHASE_ORDER_STATUS: Record<PurchaseOrderStatus, CodeInfo> = {
  DRAFT: { label: '작성', color: 'default' },
  APPROVED: { label: '승인', color: 'blue' },
  COMPLETED: { label: '입고완료', color: 'green' },
  CANCELLED: { label: '취소', color: 'red' },
}

export const STOCK_HISTORY_TYPE: Record<StockHistoryType, CodeInfo> = {
  SALE: { label: '판매', color: 'orange' },
  SALE_CANCEL: { label: '판매 취소', color: 'purple' },
  PURCHASE: { label: '입고', color: 'green' },
  ADJUSTMENT: { label: '조정', color: 'cyan' },
}

/** Select 옵션으로 변환 */
export function toOptions<K extends string>(
  codes: Record<K, CodeInfo | string>,
) {
  return (Object.keys(codes) as K[]).map((value) => {
    const info = codes[value]
    return { value, label: typeof info === 'string' ? info : info.label }
  })
}
