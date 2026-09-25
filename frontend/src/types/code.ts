// 코드 값 — docs/erd/03-code-definition.md 2장

export type Role = 'ADMIN' | 'MANAGER' | 'USER'
export type ActiveStatus = 'ACTIVE' | 'INACTIVE'
export type SaleStatus = 'COMPLETED' | 'CANCELLED'
export type PurchaseOrderStatus =
  'DRAFT' | 'APPROVED' | 'COMPLETED' | 'CANCELLED'
export type StockHistoryType =
  'SALE' | 'SALE_CANCEL' | 'PURCHASE' | 'ADJUSTMENT'
export type ReferenceType = 'SALE' | 'PURCHASE_ORDER'
