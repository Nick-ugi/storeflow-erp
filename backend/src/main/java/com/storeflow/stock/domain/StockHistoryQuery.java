package com.storeflow.stock.domain;

import com.storeflow.common.code.StockHistoryType;
import java.time.OffsetDateTime;

/**
 * 재고 이력 조회 조건 (매장 범위 · 기간이 결정된 뒤의 값). null인 조건은 적용하지 않는다.
 */
public record StockHistoryQuery(
        Long storeId,
        Long productId,
        String keyword,
        StockHistoryType type,
        OffsetDateTime from,
        OffsetDateTime to) {

    public static StockHistoryQuery recent(Long storeId, Long productId) {
        return new StockHistoryQuery(storeId, productId, null, null, null, null);
    }

}
