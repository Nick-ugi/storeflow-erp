package com.storeflow.sales.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 판매 상품. 단가는 받지 않고 서버가 현재 판매가로 계산한다.
 */
public record SaleItemRequest(
        @NotNull(message = "상품을 선택하세요.") Long productId,
        @NotNull(message = "수량을 입력하세요.")
        @Min(value = 1, message = "수량은 1 이상 9,999 이하로 입력하세요.")
        @Max(value = 9_999, message = "수량은 1 이상 9,999 이하로 입력하세요.") Integer quantity) {
}
