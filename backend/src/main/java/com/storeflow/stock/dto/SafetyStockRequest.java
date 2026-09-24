package com.storeflow.stock.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SafetyStockRequest(
        @NotNull(message = "안전재고를 입력하세요.")
        @Min(value = 0, message = "안전재고는 0 이상 99,999 이하로 입력하세요.")
        @Max(value = 99_999, message = "안전재고는 0 이상 99,999 이하로 입력하세요.") Integer safetyStock) {
}
