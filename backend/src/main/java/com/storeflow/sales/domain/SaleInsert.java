package com.storeflow.sales.domain;

import java.time.OffsetDateTime;

public record SaleInsert(String saleNumber, Long storeId, long totalAmount, OffsetDateTime soldAt, Long createdBy) {
}
