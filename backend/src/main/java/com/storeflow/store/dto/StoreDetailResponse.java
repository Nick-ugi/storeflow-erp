package com.storeflow.store.dto;

import com.storeflow.common.code.ActiveStatus;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 매장 상세 (API-STORE-003)
 */
@Getter
@Setter
public class StoreDetailResponse {

    private Long id;
    private String storeCode;
    private String storeName;
    private String address;
    private String phone;
    private ActiveStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

}
