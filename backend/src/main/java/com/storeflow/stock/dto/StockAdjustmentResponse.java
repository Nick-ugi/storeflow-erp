package com.storeflow.stock.dto;

/**
 * 조정 결과. 팝업을 연 뒤 다른 판매가 먼저 처리됐을 수 있으므로 실제 변동 전 · 후 수량을 돌려준다.
 */
public record StockAdjustmentResponse(Long productId, Long storeId, int beforeQuantity, int afterQuantity) {
}
