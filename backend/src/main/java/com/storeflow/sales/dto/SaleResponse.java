package com.storeflow.sales.dto;

import com.storeflow.common.code.SaleStatus;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 판매 목록 항목 (API-SALE-001)
 */
@Getter
@Setter
public class SaleResponse {

    private Long id;
    private String saleNumber;
    private OffsetDateTime soldAt;
    private Long storeId;
    private String storeName;
    private Integer itemCount;
    private Long totalAmount;
    private SaleStatus status;
    private String createdByName;

}
