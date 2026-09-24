package com.storeflow.supplier.dto;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 공급처 상세 (API-SUPP-003) = 목록 항목 + 생성 · 수정 일시
 */
@Getter
@Setter
public class SupplierDetailResponse extends SupplierResponse {

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

}
