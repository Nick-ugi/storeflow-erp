package com.storeflow.store.dto;

import com.storeflow.common.code.ActiveStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * 매장 목록 항목 (API-STORE-001)
 */
@Getter
@Setter
public class StoreResponse {

    private Long id;
    private String storeCode;
    private String storeName;
    private String address;
    private String phone;
    private ActiveStatus status;

}
