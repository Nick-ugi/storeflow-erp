package com.storeflow.stock.dto;

import com.storeflow.common.util.Texts;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 재고 조정 (API-STOCK-004). quantity는 증가 양수, 감소 음수 (절댓값 1 ~ 9,999는 서비스에서 검사)
 */
public record StockAdjustmentRequest(
        Long storeId,
        @NotNull(message = "상품을 선택하세요.") Long productId,
        @NotNull(message = "조정 수량을 입력하세요.") Integer quantity,
        @NotBlank(message = "조정 사유를 입력하세요.")
        @Size(max = 200, message = "조정 사유는 200자 이하로 입력하세요.") String reason) {

    public StockAdjustmentRequest {
        reason = Texts.trim(reason);
    }

}
