package com.storeflow.sales.domain;

import com.storeflow.common.code.SaleStatus;
import java.time.OffsetDateTime;

/**
 * 판매 목록 조회 조건 (매장 범위 · 기간이 결정된 뒤의 값). null인 조건은 적용하지 않는다.
 */
public record SaleQuery(Long storeId, String saleNumber, SaleStatus status, OffsetDateTime from, OffsetDateTime to) {
}
