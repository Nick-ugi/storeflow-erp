package com.storeflow.purchase.dto;

import com.storeflow.common.code.PurchaseOrderStatus;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 발주 목록 항목 (API-PO-001)
 */
@Getter
@Setter
public class PurchaseOrderResponse {

    private Long id;
    private String orderNumber;
    private OffsetDateTime orderedAt;
    private Long storeId;
    private String storeName;
    private Long supplierId;
    private String supplierName;
    private Integer itemCount;
    private Long totalAmount;
    private PurchaseOrderStatus status;

}
