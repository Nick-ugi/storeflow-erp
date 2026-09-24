package com.storeflow.purchase.domain;

import java.time.OffsetDateTime;

public record PurchaseOrderInsert(
        String orderNumber,
        Long storeId,
        Long supplierId,
        long totalAmount,
        OffsetDateTime orderedAt,
        Long createdBy) {
}
