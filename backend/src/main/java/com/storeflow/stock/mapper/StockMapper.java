package com.storeflow.stock.mapper;

import com.storeflow.stock.domain.LockedStock;
import com.storeflow.stock.domain.StockHistoryInsert;
import com.storeflow.stock.domain.StockHistoryQuery;
import com.storeflow.stock.dto.StockDetailResponse;
import com.storeflow.stock.dto.StockHistoryResponse;
import com.storeflow.stock.dto.StockResponse;
import com.storeflow.stock.dto.StockSearchRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StockMapper {

    /** 매장의 대상 재고 행을 상품 ID 오름차순으로 잠근다. (교착 상태 방지) */
    List<LockedStock> lockStocks(@Param("storeId") Long storeId, @Param("productIds") List<Long> productIds);

    int updateQuantity(@Param("stockId") Long stockId, @Param("quantity") int quantity, @Param("updatedBy") Long updatedBy);

    int insertHistory(StockHistoryInsert history);

    List<StockResponse> findStocks(@Param("cond") StockSearchRequest cond, @Param("storeId") Long storeId,
                                   @Param("offset") int offset, @Param("limit") int limit);

    long countStocks(@Param("cond") StockSearchRequest cond, @Param("storeId") Long storeId);

    StockDetailResponse findStock(@Param("storeId") Long storeId, @Param("productId") Long productId);

    List<StockHistoryResponse> findHistories(@Param("cond") StockHistoryQuery cond, @Param("offset") int offset, @Param("limit") int limit);

    long countHistories(@Param("cond") StockHistoryQuery cond);

    int updateSafetyStock(@Param("storeId") Long storeId, @Param("productId") Long productId,
                          @Param("safetyStock") int safetyStock, @Param("updatedBy") Long updatedBy);

}
