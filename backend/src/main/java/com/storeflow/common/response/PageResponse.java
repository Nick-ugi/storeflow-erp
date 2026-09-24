package com.storeflow.common.response;

import com.storeflow.common.request.PageParams;
import java.util.List;

/**
 * 목록 응답 (API 공통 규칙 4.2)
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(List<T> content, PageParams params, long totalElements) {
        int size = params.pageSize();
        int totalPages = (int) ((totalElements + size - 1) / size);
        return new PageResponse<>(content, params.pageNumber(), size, totalElements, totalPages);
    }

}
