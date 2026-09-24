package com.storeflow.purchase.dto;

import com.storeflow.common.code.PurchaseOrderStatus;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 발주 상세 (API-PO-002). 처리되지 않은 단계의 일시 · 처리자는 null
 */
@Getter
@Setter
public class PurchaseOrderDetailResponse {

    private Long id;
    private String orderNumber;
    private Long storeId;
    private String storeName;
    private Long supplierId;
    private String supplierName;
    private PurchaseOrderStatus status;
    private Long totalAmount;
    private OffsetDateTime orderedAt;
    private String createdByName;
    private OffsetDateTime approvedAt;
    private String approvedByName;
    private OffsetDateTime completedAt;
    private String completedByName;
    private OffsetDateTime cancelledAt;
    private String cancelledByName;
    private OffsetDateTime updatedAt;
    private String updatedByName;
    private List<PurchaseOrderItemResponse> items;

}
