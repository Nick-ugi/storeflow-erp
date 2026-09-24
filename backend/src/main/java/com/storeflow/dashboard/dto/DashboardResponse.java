package com.storeflow.dashboard.dto;

import java.util.List;

/**
 * Dashboard 전체 데이터 (API-DASH-001). recentPurchaseOrders는 USER에게 null
 */
public record DashboardResponse(
        Long storeId,
        SalesSummary todaySales,
        SalesSummary monthSales,
        List<DailySales> salesTrend,
        StockSummary stockSummary,
        List<ShortageStock> shortageStocks,
        List<RecentSale> recentSales,
        List<RecentPurchaseOrder> recentPurchaseOrders) {
}
