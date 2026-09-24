package com.storeflow.stock.domain;

import com.storeflow.common.code.ReferenceType;
import com.storeflow.common.code.StockHistoryType;

public record StockHistoryInsert(
        Long storeId,
        Long productId,
        StockHistoryType type,
        int quantity,
        int beforeQuantity,
        int afterQuantity,
        ReferenceType referenceType,
        Long referenceId,
        String reason,
        Long createdBy) {
}
