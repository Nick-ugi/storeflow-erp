package com.storeflow.stock.domain;

public record StockChangeResult(Long productId, int beforeQuantity, int afterQuantity) {
}
