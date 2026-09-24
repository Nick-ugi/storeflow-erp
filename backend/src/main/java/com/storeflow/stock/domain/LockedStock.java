package com.storeflow.stock.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * SELECT … FOR UPDATE로 잠근 재고 행
 */
@Getter
@Setter
public class LockedStock {

    private Long stockId;
    private Long productId;
    private String productName;
    private int quantity;

}
