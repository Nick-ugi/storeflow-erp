package com.storeflow.stock.dto;

public record SafetyStockResponse(Long productId, Long storeId, int safetyStock) {
}
