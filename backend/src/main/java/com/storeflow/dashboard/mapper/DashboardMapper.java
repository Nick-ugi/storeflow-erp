package com.storeflow.dashboard.mapper;

import com.storeflow.dashboard.dto.DailySales;
import com.storeflow.dashboard.dto.RecentPurchaseOrder;
import com.storeflow.dashboard.dto.RecentSale;
import com.storeflow.dashboard.dto.SalesSummary;
import com.storeflow.dashboard.dto.ShortageStock;
import com.storeflow.dashboard.dto.StockSummary;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * storeId가 null이면 전 매장 기준으로 집계한다.
 */
@Mapper
public interface DashboardMapper {

    SalesSummary sumSales(@Param("storeId") Long storeId, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    List<DailySales> dailySales(@Param("storeId") Long storeId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    StockSummary stockSummary(@Param("storeId") Long storeId);

    List<ShortageStock> findShortageStocks(@Param("storeId") Long storeId, @Param("limit") int limit);

    List<RecentSale> findRecentSales(@Param("storeId") Long storeId, @Param("limit") int limit);

    List<RecentPurchaseOrder> findRecentPurchaseOrders(@Param("storeId") Long storeId, @Param("limit") int limit);

}
