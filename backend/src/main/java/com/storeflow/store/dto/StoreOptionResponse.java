package com.storeflow.store.dto;

import com.storeflow.common.code.ActiveStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * 매장 선택 목록 항목 (API-STORE-002). 사용 중지 매장도 포함하며 화면이 용도에 따라 거른다.
 */
@Getter
@Setter
public class StoreOptionResponse {

    private Long id;
    private String storeCode;
    private String storeName;
    private ActiveStatus status;

}
