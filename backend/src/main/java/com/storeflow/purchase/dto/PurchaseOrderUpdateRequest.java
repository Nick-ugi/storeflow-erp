package com.storeflow.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 발주 수정 (API-PO-004). 매장은 바꿀 수 없으므로 받지 않고, 발주 상품은 전체 교체한다.
 */
public record PurchaseOrderUpdateRequest(
        @NotNull(message = "공급처를 선택하세요.") Long supplierId,
        @NotEmpty(message = "발주할 상품을 1개 이상 추가하세요.")
        @Size(max = 50, message = "한 번에 최대 50개 상품까지 발주할 수 있습니다.")
        List<@NotNull(message = "발주할 상품을 1개 이상 추가하세요.") @Valid PurchaseOrderItemRequest> items) {
}
