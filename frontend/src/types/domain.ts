// API 요청 · 응답 데이터 — docs/api/03 ~ 06 상세 명세와 같은 이름을 쓴다.
import type {
  ActiveStatus,
  PurchaseOrderStatus,
  ReferenceType,
  Role,
  SaleStatus,
  StockHistoryType,
} from '@/types/code'

// ===== 인증 =====
export interface UserInfo {
  id: number
  username: string
  name: string
  role: Role
  storeId: number | null
  storeName: string | null
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: UserInfo
}

// ===== 매장 =====
export interface Store {
  id: number
  storeCode: string
  storeName: string
  address: string | null
  phone: string | null
  status: ActiveStatus
}

export interface StoreDetail extends Store {
  createdAt: string
  updatedAt: string
}

export interface StoreOption {
  id: number
  storeCode: string
  storeName: string
  status: ActiveStatus
}

export interface StoreRequest {
  storeCode?: string
  storeName: string
  address?: string | null
  phone?: string | null
}

// ===== 사용자 =====
export interface User {
  id: number
  username: string
  name: string
  role: Role
  storeId: number | null
  storeName: string | null
  status: ActiveStatus
  createdAt: string
}

export interface UserDetail extends User {
  updatedAt: string
}

export interface UserRequest {
  username?: string
  password?: string
  name: string
  role: Role
  storeId: number | null
}

// ===== 카테고리 =====
export interface Category {
  id: number
  categoryName: string
  description: string | null
}

export interface CategoryRequest {
  categoryName: string
  description?: string | null
}

// ===== 상품 =====
export interface Product {
  id: number
  productCode: string
  productName: string
  categoryId: number
  categoryName: string
  purchasePrice: number
  salePrice: number
  stockQuantity: number
  status: ActiveStatus
}

export interface ProductDetail {
  id: number
  productCode: string
  productName: string
  categoryId: number
  categoryName: string
  purchasePrice: number
  salePrice: number
  status: ActiveStatus
  createdAt: string
  updatedAt: string
}

export interface ProductRequest {
  productCode?: string
  productName: string
  categoryId: number
  purchasePrice: number
  salePrice: number
}

// ===== 공급처 =====
export interface Supplier {
  id: number
  supplierName: string
  businessNumber: string
  contactName: string | null
  phone: string | null
  status: ActiveStatus
}

export interface SupplierDetail extends Supplier {
  createdAt: string
  updatedAt: string
}

export interface SupplierOption {
  id: number
  supplierName: string
  status: ActiveStatus
}

export interface SupplierRequest {
  supplierName: string
  businessNumber: string
  contactName?: string | null
  phone?: string | null
}

// ===== 판매 =====
export interface Sale {
  id: number
  saleNumber: string
  soldAt: string
  storeId: number
  storeName: string
  itemCount: number
  totalAmount: number
  status: SaleStatus
  createdByName: string
}

export interface SaleItem {
  productId: number
  productCode: string
  productName: string
  quantity: number
  unitPrice: number
  totalPrice: number
}

export interface SaleDetail {
  id: number
  saleNumber: string
  storeId: number
  storeName: string
  soldAt: string
  status: SaleStatus
  totalAmount: number
  createdByName: string
  cancelledAt: string | null
  cancelledByName: string | null
  items: SaleItem[]
}

export interface SaleCreateRequest {
  storeId: number | null
  items: { productId: number; quantity: number }[]
}

export interface SaleCreateResponse {
  id: number
  saleNumber: string
}

export interface SaleCancelResponse {
  id: number
  status: SaleStatus
  cancelledAt: string
}

// ===== 재고 =====
export interface Stock {
  storeId: number
  storeName: string
  productId: number
  productCode: string
  productName: string
  categoryName: string
  productStatus: ActiveStatus
  quantity: number
  safetyStock: number
  shortage: boolean
}

export interface StockHistory {
  id: number
  createdAt: string
  storeId: number
  storeName: string
  productId: number
  productCode: string
  productName: string
  type: StockHistoryType
  quantity: number
  beforeQuantity: number
  afterQuantity: number
  referenceType: ReferenceType | null
  referenceId: number | null
  referenceNumber: string | null
  reason: string | null
  createdByName: string
}

export interface StockDetail extends Stock {
  recentHistories: StockHistory[]
}

export interface StockAdjustmentRequest {
  storeId: number | null
  productId: number
  quantity: number
  reason: string
}

export interface StockAdjustmentResponse {
  productId: number
  storeId: number
  beforeQuantity: number
  afterQuantity: number
}

export interface SafetyStockResponse {
  productId: number
  storeId: number
  safetyStock: number
}

// ===== 발주 =====
export interface PurchaseOrder {
  id: number
  orderNumber: string
  orderedAt: string
  storeId: number
  storeName: string
  supplierId: number
  supplierName: string
  itemCount: number
  totalAmount: number
  status: PurchaseOrderStatus
}

export interface PurchaseOrderItem {
  productId: number
  productCode: string
  productName: string
  quantity: number
  unitPrice: number
  totalPrice: number
}

export interface PurchaseOrderDetail {
  id: number
  orderNumber: string
  storeId: number
  storeName: string
  supplierId: number
  supplierName: string
  status: PurchaseOrderStatus
  totalAmount: number
  orderedAt: string
  createdByName: string
  approvedAt: string | null
  approvedByName: string | null
  completedAt: string | null
  completedByName: string | null
  cancelledAt: string | null
  cancelledByName: string | null
  updatedAt: string
  updatedByName: string | null
  items: PurchaseOrderItem[]
}

export interface PurchaseOrderRequest {
  storeId?: number | null
  supplierId: number
  items: { productId: number; quantity: number; unitPrice: number }[]
}

export interface PurchaseOrderCreateResponse {
  id: number
  orderNumber: string
}

// ===== Dashboard =====
export interface SalesSummary {
  amount: number
  count: number
}

export interface Dashboard {
  storeId: number | null
  todaySales: SalesSummary
  monthSales: SalesSummary
  salesTrend: { date: string; amount: number }[]
  stockSummary: { inStockProductCount: number; shortageCount: number }
  shortageStocks: {
    storeId: number
    storeName: string
    productId: number
    productCode: string
    productName: string
    quantity: number
    safetyStock: number
    shortageQuantity: number
  }[]
  recentSales: {
    id: number
    saleNumber: string
    soldAt: string
    totalAmount: number
    status: SaleStatus
  }[]
  recentPurchaseOrders:
    | {
        id: number
        orderNumber: string
        supplierName: string
        orderedAt: string
        status: PurchaseOrderStatus
      }[]
    | null
}
