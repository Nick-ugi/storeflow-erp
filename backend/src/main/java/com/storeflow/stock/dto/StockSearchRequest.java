package com.storeflow.stock.dto;

import com.storeflow.common.request.PageParams;
import com.storeflow.common.util.Texts;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 현재 재고 조건 (API-STOCK-001). storeId는 ADMIN만 적용된다.
 */
public record StockSearchRequest(
        String keyword,
        Long categoryId,
        Boolean shortageOnly,
        Long storeId,
        @Min(value = 0, message = PageParams.PAGE_MESSAGE) Integer page,
        @Min(value = 1, message = PageParams.SIZE_MESSAGE) @Max(value = 100, message = PageParams.SIZE_MESSAGE) Integer size)
        implements PageParams {

    public StockSearchRequest {
        keyword = Texts.trimToNull(keyword);
        shortageOnly = Boolean.TRUE.equals(shortageOnly);
    }

}
