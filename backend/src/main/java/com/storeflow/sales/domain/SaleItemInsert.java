package com.storeflow.sales.domain;

public record SaleItemInsert(Long productId, int quantity, long unitPrice, long totalPrice) {

    /** 판매 시점 판매가로 금액을 계산한다. (BR-041, BR-042) */
    public static SaleItemInsert of(Long productId, int quantity, long unitPrice) {
        return new SaleItemInsert(productId, quantity, unitPrice, unitPrice * quantity);
    }

}
