package com.storeflow.stock.dto;

import com.storeflow.common.code.ActiveStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * 현재 재고 항목 (API-STOCK-001)
 */
@Getter
@Setter
public class StockResponse {

    private Long storeId;
    private String storeName;
    private Long productId;
    private String productCode;
    private String productName;
    private String categoryName;
    private ActiveStatus productStatus;
    private Integer quantity;
    private Integer safetyStock;
    /** 사용 중인 상품이고 현재 수량 &lt; 안전재고 (BR-035) */
    private Boolean shortage;

}
