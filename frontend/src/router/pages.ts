import { lazy } from 'react'

// 화면별로 코드를 나눠 필요한 화면의 코드만 내려받는다. (로그인 · 오류 화면은 바로 포함)
export const CategoryPage = lazy(() =>
  import('@/pages/categories/CategoryPage').then((m) => ({
    default: m.CategoryPage,
  })),
)
export const DashboardPage = lazy(() =>
  import('@/pages/dashboard/DashboardPage').then((m) => ({
    default: m.DashboardPage,
  })),
)
export const ProductDetailPage = lazy(() =>
  import('@/pages/products/ProductDetailPage').then((m) => ({
    default: m.ProductDetailPage,
  })),
)
export const ProductFormPage = lazy(() =>
  import('@/pages/products/ProductFormPage').then((m) => ({
    default: m.ProductFormPage,
  })),
)
export const ProductListPage = lazy(() =>
  import('@/pages/products/ProductListPage').then((m) => ({
    default: m.ProductListPage,
  })),
)
export const PurchaseOrderDetailPage = lazy(() =>
  import('@/pages/purchases/PurchaseOrderDetailPage').then((m) => ({
    default: m.PurchaseOrderDetailPage,
  })),
)
export const PurchaseOrderFormPage = lazy(() =>
  import('@/pages/purchases/PurchaseOrderFormPage').then((m) => ({
    default: m.PurchaseOrderFormPage,
  })),
)
export const PurchaseOrderListPage = lazy(() =>
  import('@/pages/purchases/PurchaseOrderListPage').then((m) => ({
    default: m.PurchaseOrderListPage,
  })),
)
export const SaleCreatePage = lazy(() =>
  import('@/pages/sales/SaleCreatePage').then((m) => ({
    default: m.SaleCreatePage,
  })),
)
export const SaleDetailPage = lazy(() =>
  import('@/pages/sales/SaleDetailPage').then((m) => ({
    default: m.SaleDetailPage,
  })),
)
export const SaleListPage = lazy(() =>
  import('@/pages/sales/SaleListPage').then((m) => ({
    default: m.SaleListPage,
  })),
)
export const StockDetailPage = lazy(() =>
  import('@/pages/stocks/StockDetailPage').then((m) => ({
    default: m.StockDetailPage,
  })),
)
export const StockHistoryPage = lazy(() =>
  import('@/pages/stocks/StockHistoryPage').then((m) => ({
    default: m.StockHistoryPage,
  })),
)
export const StockListPage = lazy(() =>
  import('@/pages/stocks/StockListPage').then((m) => ({
    default: m.StockListPage,
  })),
)
export const StorePage = lazy(() =>
  import('@/pages/stores/StorePage').then((m) => ({ default: m.StorePage })),
)
export const SupplierPage = lazy(() =>
  import('@/pages/suppliers/SupplierPage').then((m) => ({
    default: m.SupplierPage,
  })),
)
export const UserPage = lazy(() =>
  import('@/pages/users/UserPage').then((m) => ({ default: m.UserPage })),
)
