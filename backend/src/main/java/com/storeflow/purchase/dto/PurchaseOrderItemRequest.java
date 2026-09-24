package com.storeflow.purchase.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 발주 상품. 발주 단가는 사용자가 정한 값을 그대로 쓴다. (화면 기본값: 상품 매입가)
 */
public record PurchaseOrderItemRequest(
        @NotNull(message = "상품을 선택하세요.") Long productId,
        @NotNull(message = "수량을 입력하세요.")
        @Min(value = 1, message = "수량은 1 이상 99,999 이하로 입력하세요.")
        @Max(value = 99_999, message = "수량은 1 이상 99,999 이하로 입력하세요.") Integer quantity,
        @NotNull(message = "단가를 입력하세요.")
        @Min(value = 0, message = "단가는 0 이상 99,999,999 이하로 입력하세요.")
        @Max(value = 99_999_999, message = "단가는 0 이상 99,999,999 이하로 입력하세요.") Long unitPrice) {
}
