package com.storeflow.product.domain;

import com.storeflow.common.code.ActiveStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * 판매 · 발주 등록 시점의 상품 정보 (이름, 가격, 상태)
 */
@Getter
@Setter
public class ProductSnapshot {

    private Long id;
    private String productName;
    private Long purchasePrice;
    private Long salePrice;
    private ActiveStatus status;

}
