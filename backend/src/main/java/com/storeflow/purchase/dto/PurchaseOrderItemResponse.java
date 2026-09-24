package com.storeflow.purchase.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PurchaseOrderItemResponse {

    private Long productId;
    private String productCode;
    private String productName;
    private Integer quantity;
    private Long unitPrice;
    private Long totalPrice;

}
