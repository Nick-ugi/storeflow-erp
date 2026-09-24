package com.storeflow.product.dto;

import com.storeflow.common.code.ActiveStatus;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 상품 상세 (API-PROD-002)
 */
@Getter
@Setter
public class ProductDetailResponse {

    private Long id;
    private String productCode;
    private String productName;
    private Long categoryId;
    private String categoryName;
    private Long purchasePrice;
    private Long salePrice;
    private ActiveStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

}
