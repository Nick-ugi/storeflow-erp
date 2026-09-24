package com.storeflow.purchase.domain;

import com.storeflow.purchase.dto.PurchaseOrderItemRequest;

public record PurchaseOrderItemInsert(Long productId, int quantity, long unitPrice, long totalPrice) {

    /** 금액 = 단가 × 수량 (서버에서 다시 계산) */
    public static PurchaseOrderItemInsert from(PurchaseOrderItemRequest item) {
        return new PurchaseOrderItemInsert(item.productId(), item.quantity(), item.unitPrice(), item.unitPrice() * item.quantity());
    }

}
