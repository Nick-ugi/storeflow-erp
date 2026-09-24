package com.storeflow.purchase.domain;

import com.storeflow.common.code.PurchaseOrderStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * SELECT … FOR UPDATE로 잠근 발주 행 (상태 확인용)
 */
@Getter
@Setter
public class LockedPurchaseOrder {

    private Long id;
    private Long storeId;
    private PurchaseOrderStatus status;

}
