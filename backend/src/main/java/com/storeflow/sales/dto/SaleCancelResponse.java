package com.storeflow.sales.dto;

import com.storeflow.common.code.SaleStatus;
import java.time.OffsetDateTime;

public record SaleCancelResponse(Long id, SaleStatus status, OffsetDateTime cancelledAt) {
}
