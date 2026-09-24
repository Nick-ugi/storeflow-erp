package com.storeflow.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 재고 보유 상품 수(수량 1 이상), 부족 재고 건수(매장 × 상품)
 */
@Getter
@Setter
public class StockSummary {

    private Long inStockProductCount;
    private Long shortageCount;

}
