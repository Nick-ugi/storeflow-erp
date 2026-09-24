package com.storeflow.purchase.dto;

import com.storeflow.common.code.PurchaseOrderStatus;
import com.storeflow.common.request.PageParams;
import com.storeflow.common.util.Texts;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 발주 목록 조건 (API-PO-001). 기간 기본값은 최근 30일, 발주번호가 있으면 기간 조건은 적용하지 않는다.
 */
public record PurchaseOrderSearchRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        PurchaseOrderStatus status,
        Long supplierId,
        String orderNumber,
        Long storeId,
        @Min(value = 0, message = PageParams.PAGE_MESSAGE) Integer page,
        @Min(value = 1, message = PageParams.SIZE_MESSAGE) @Max(value = 100, message = PageParams.SIZE_MESSAGE) Integer size)
        implements PageParams {

    public PurchaseOrderSearchRequest {
        orderNumber = Texts.trimToNull(orderNumber);
    }

}
