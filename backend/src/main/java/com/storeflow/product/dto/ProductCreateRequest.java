package com.storeflow.product.dto;

import com.storeflow.common.util.Texts;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductCreateRequest(
        @NotBlank(message = "상품코드를 입력하세요.")
        @Pattern(regexp = "^[A-Z0-9-]{1,20}$", message = "상품코드는 영문 대문자, 숫자, 하이픈(-)으로 20자 이하로 입력하세요.") String productCode,
        @NotBlank(message = "상품명을 입력하세요.")
        @Size(max = 100, message = "상품명은 100자 이하로 입력하세요.") String productName,
        @NotNull(message = "카테고리를 선택하세요.") Long categoryId,
        @NotNull(message = "매입가를 입력하세요.")
        @Min(value = 0, message = ProductPrice.MESSAGE) @Max(value = ProductPrice.MAX, message = ProductPrice.MESSAGE) Long purchasePrice,
        @NotNull(message = "판매가를 입력하세요.")
        @Min(value = 0, message = ProductPrice.MESSAGE) @Max(value = ProductPrice.MAX, message = ProductPrice.MESSAGE) Long salePrice) {

    public ProductCreateRequest {
        productCode = Texts.trim(productCode);
        productName = Texts.trim(productName);
    }

}
