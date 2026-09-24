package com.storeflow.stock.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 재고 상세 (API-STOCK-002) = 재고 항목 + 최근 변동 이력 10건
 */
@Getter
@Setter
public class StockDetailResponse extends StockResponse {

    private List<StockHistoryResponse> recentHistories;

}
