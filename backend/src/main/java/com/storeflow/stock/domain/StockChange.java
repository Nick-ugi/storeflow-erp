package com.storeflow.stock.domain;

/**
 * 상품 하나의 재고 변동. quantity는 증가 +, 감소 −
 */
public record StockChange(Long productId, int quantity) {
}
