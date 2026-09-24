package com.storeflow.category.dto;

import com.storeflow.common.util.Texts;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 카테고리 등록 · 수정 (API-CAT-002, 003)
 */
public record CategoryRequest(
        @NotBlank(message = "카테고리명을 입력하세요.")
        @Size(max = 50, message = "카테고리명은 50자, 설명은 200자 이하로 입력하세요.") String categoryName,
        @Size(max = 200, message = "카테고리명은 50자, 설명은 200자 이하로 입력하세요.") String description) {

    public CategoryRequest {
        categoryName = Texts.trim(categoryName);
        description = Texts.trimToNull(description);
    }

}
