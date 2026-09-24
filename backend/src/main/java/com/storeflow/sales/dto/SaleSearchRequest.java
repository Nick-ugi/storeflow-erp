package com.storeflow.sales.dto;

import com.storeflow.common.code.SaleStatus;
import com.storeflow.common.request.PageParams;
import com.storeflow.common.util.Texts;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 판매 목록 조건 (API-SALE-001). 판매번호가 있으면 기간 조건은 적용하지 않는다.
 */
public record SaleSearchRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        String saleNumber,
        SaleStatus status,
        Long storeId,
        @Min(value = 0, message = PageParams.PAGE_MESSAGE) Integer page,
        @Min(value = 1, message = PageParams.SIZE_MESSAGE) @Max(value = 100, message = PageParams.SIZE_MESSAGE) Integer size)
        implements PageParams {

    public SaleSearchRequest {
        saleNumber = Texts.trimToNull(saleNumber);
    }

}
