package com.storeflow.dashboard.dto;

import com.storeflow.common.code.PurchaseOrderStatus;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecentPurchaseOrder {

    private Long id;
    private String orderNumber;
    private String supplierName;
    private OffsetDateTime orderedAt;
    private PurchaseOrderStatus status;

}
