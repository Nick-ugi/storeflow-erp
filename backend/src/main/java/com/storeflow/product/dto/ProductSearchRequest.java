package com.storeflow.product.dto;

import com.storeflow.common.code.ActiveStatus;
import com.storeflow.common.request.PageParams;
import com.storeflow.common.util.Texts;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 상품 목록 조건 (API-PROD-001). storeId는 재고 수량 기준 매장이며 ADMIN만 적용된다.
 */
public record ProductSearchRequest(
        String keyword,
        Long categoryId,
        ActiveStatus status,
        Long storeId,
        @Min(value = 0, message = PageParams.PAGE_MESSAGE) Integer page,
        @Min(value = 1, message = PageParams.SIZE_MESSAGE) @Max(value = 100, message = PageParams.SIZE_MESSAGE) Integer size)
        implements PageParams {

    public ProductSearchRequest {
        keyword = Texts.trimToNull(keyword);
    }

}
