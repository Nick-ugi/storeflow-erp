package com.storeflow.supplier.dto;

import com.storeflow.common.code.ActiveStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * 공급처 선택 목록 항목 (API-SUPP-002). 사용 중지 공급처도 포함한다.
 */
@Getter
@Setter
public class SupplierOptionResponse {

    private Long id;
    private String supplierName;
    private ActiveStatus status;

}
