package com.storeflow.stock.dto;

import com.storeflow.common.code.StockHistoryType;
import com.storeflow.common.request.PageParams;
import com.storeflow.common.util.Texts;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 재고 이력 조건 (API-STOCK-003). 기간 기본값은 최근 7일, 최대 1년
 */
public record StockHistorySearchRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        String keyword,
        Long productId,
        StockHistoryType type,
        Long storeId,
        @Min(value = 0, message = PageParams.PAGE_MESSAGE) Integer page,
        @Min(value = 1, message = PageParams.SIZE_MESSAGE) @Max(value = 100, message = PageParams.SIZE_MESSAGE) Integer size)
        implements PageParams {

    public StockHistorySearchRequest {
        keyword = Texts.trimToNull(keyword);
    }

}
