package com.storeflow.stock.dto;

import com.storeflow.common.code.ReferenceType;
import com.storeflow.common.code.StockHistoryType;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 재고 이력 항목 (API-STOCK-003, 재고 상세의 최근 이력)
 */
@Getter
@Setter
public class StockHistoryResponse {

    private Long id;
    private OffsetDateTime createdAt;
    private Long storeId;
    private String storeName;
    private Long productId;
    private String productCode;
    private String productName;
    private StockHistoryType type;
    private Integer quantity;
    private Integer beforeQuantity;
    private Integer afterQuantity;
    private ReferenceType referenceType;
    private Long referenceId;
    /** 관련 문서의 판매번호 또는 발주번호 (화면 링크 표시용) */
    private String referenceNumber;
    private String reason;
    private String createdByName;

}
