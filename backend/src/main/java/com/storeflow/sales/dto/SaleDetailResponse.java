package com.storeflow.sales.dto;

import com.storeflow.common.code.SaleStatus;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 판매 상세 (API-SALE-002)
 */
@Getter
@Setter
public class SaleDetailResponse {

    private Long id;
    private String saleNumber;
    private Long storeId;
    private String storeName;
    private OffsetDateTime soldAt;
    private SaleStatus status;
    private Long totalAmount;
    private String createdByName;
    private OffsetDateTime cancelledAt;
    private String cancelledByName;
    private List<SaleItemResponse> items;

}
