package com.storeflow.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 판매 등록 (API-SALE-003). storeId는 ADMIN만 사용한다.
 */
public record SaleCreateRequest(
        Long storeId,
        @NotEmpty(message = "판매할 상품을 1개 이상 추가하세요.")
        @Size(max = 50, message = "한 번에 최대 50개 상품까지 판매할 수 있습니다.")
        List<@NotNull(message = "판매할 상품을 1개 이상 추가하세요.") @Valid SaleItemRequest> items) {
}
