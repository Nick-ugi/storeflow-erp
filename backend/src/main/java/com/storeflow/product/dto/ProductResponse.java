package com.storeflow.product.dto;

import com.storeflow.common.code.ActiveStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * 상품 목록 항목 (API-PROD-001). 상품 선택 팝업도 같은 응답을 사용한다.
 */
@Getter
@Setter
public class ProductResponse {

    private Long id;
    private String productCode;
    private String productName;
    private Long categoryId;
    private String categoryName;
    private Long purchasePrice;
    private Long salePrice;
    /** MANAGER · USER: 소속 매장 수량 / ADMIN: 선택 매장 수량, 매장 미선택 시 전 매장 합계 */
    private Integer stockQuantity;
    private ActiveStatus status;

}
