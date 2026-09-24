package com.storeflow.supplier.dto;

import com.storeflow.common.code.ActiveStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * 공급처 목록 항목 (API-SUPP-001)
 */
@Getter
@Setter
public class SupplierResponse {

    private Long id;
    private String supplierName;
    private String businessNumber;
    private String contactName;
    private String phone;
    private ActiveStatus status;

}
