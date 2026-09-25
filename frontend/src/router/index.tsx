import { createBrowserRouter, Navigate } from 'react-router'
import { AppLayout } from '@/components/layout/AppLayout'
import { LoginPage } from '@/pages/auth/LoginPage'
import { ErrorPage } from '@/pages/ErrorPage'
import { RequireAuth, RequireRole } from '@/router/guards'
import {
  CategoryPage,
  DashboardPage,
  ProductDetailPage,
  ProductFormPage,
  ProductListPage,
  PurchaseOrderDetailPage,
  PurchaseOrderFormPage,
  PurchaseOrderListPage,
  SaleCreatePage,
  SaleDetailPage,
  SaleListPage,
  StockDetailPage,
  StockHistoryPage,
  StockListPage,
  StorePage,
  SupplierPage,
  UserPage,
} from '@/router/pages'

/** 화면 URL과 역할 — 화면 목록 2장 · 4장 */
export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    path: '/',
    element: (
      <RequireAuth>
        <AppLayout />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: 'dashboard', element: <DashboardPage /> },

      // 판매 (SCR-SALE)
      { path: 'sales', element: <SaleListPage /> },
      { path: 'sales/new', element: <SaleCreatePage /> },
      { path: 'sales/:saleId', element: <SaleDetailPage /> },

      // 재고 (SCR-STOCK)
      { path: 'stocks', element: <StockListPage /> },
      { path: 'stocks/histories', element: <StockHistoryPage /> },
      { path: 'stocks/:productId', element: <StockDetailPage /> },

      // 발주 (SCR-PO) — ADMIN · MANAGER
      {
        path: 'purchase-orders',
        element: (
          <RequireRole roles={['ADMIN', 'MANAGER']}>
            <PurchaseOrderListPage />
          </RequireRole>
        ),
      },
      {
        path: 'purchase-orders/new',
        element: (
          <RequireRole roles={['ADMIN', 'MANAGER']}>
            <PurchaseOrderFormPage />
          </RequireRole>
        ),
      },
      {
        path: 'purchase-orders/:purchaseOrderId',
        element: (
          <RequireRole roles={['ADMIN', 'MANAGER']}>
            <PurchaseOrderDetailPage />
          </RequireRole>
        ),
      },
      {
        path: 'purchase-orders/:purchaseOrderId/edit',
        element: (
          <RequireRole roles={['ADMIN', 'MANAGER']}>
            <PurchaseOrderFormPage />
          </RequireRole>
        ),
      },

      // 기준정보 (SCR-PROD, SCR-CAT, SCR-SUPP) — 조회는 전 역할, 등록 · 수정은 ADMIN
      { path: 'products', element: <ProductListPage /> },
      {
        path: 'products/new',
        element: (
          <RequireRole roles={['ADMIN']}>
            <ProductFormPage />
          </RequireRole>
        ),
      },
      { path: 'products/:productId', element: <ProductDetailPage /> },
      {
        path: 'products/:productId/edit',
        element: (
          <RequireRole roles={['ADMIN']}>
            <ProductFormPage />
          </RequireRole>
        ),
      },
      { path: 'categories', element: <CategoryPage /> },
      { path: 'suppliers', element: <SupplierPage /> },

      // 시스템 관리 (SCR-STORE, SCR-USER) — ADMIN
      {
        path: 'stores',
        element: (
          <RequireRole roles={['ADMIN']}>
            <StorePage />
          </RequireRole>
        ),
      },
      {
        path: 'users',
        element: (
          <RequireRole roles={['ADMIN']}>
            <UserPage />
          </RequireRole>
        ),
      },

      // 오류 안내 (SCR-COM-002)
      { path: '403', element: <ErrorPage status={403} /> },
      { path: '*', element: <ErrorPage status={404} /> },
    ],
  },
])
