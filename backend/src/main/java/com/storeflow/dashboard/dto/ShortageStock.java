package com.storeflow.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShortageStock {

    private Long storeId;
    private String storeName;
    private Long productId;
    private String productCode;
    private String productName;
    private Integer quantity;
    private Integer safetyStock;
    /** 안전재고 − 현재 수량 */
    private Integer shortageQuantity;

}
