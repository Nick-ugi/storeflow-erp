package com.storeflow.purchase.domain;

import com.storeflow.common.code.PurchaseOrderStatus;
import java.time.OffsetDateTime;

/**
 * 발주 목록 조회 조건 (매장 범위 · 기간이 결정된 뒤의 값). null인 조건은 적용하지 않는다.
 */
public record PurchaseOrderQuery(
        Long storeId,
        Long supplierId,
        String orderNumber,
        PurchaseOrderStatus status,
        OffsetDateTime from,
        OffsetDateTime to) {
}
