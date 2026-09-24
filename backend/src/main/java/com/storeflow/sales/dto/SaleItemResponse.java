package com.storeflow.sales.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 판매 상세 상품. unitPrice는 판매 시점의 판매가다. (BR-041)
 */
@Getter
@Setter
public class SaleItemResponse {

    private Long productId;
    private String productCode;
    private String productName;
    private Integer quantity;
    private Long unitPrice;
    private Long totalPrice;

}
