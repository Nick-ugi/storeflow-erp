package com.storeflow.store.dto;

import com.storeflow.common.util.Texts;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StoreCreateRequest(
        @NotBlank(message = "매장코드를 입력하세요.")
        @Pattern(regexp = "^[A-Z0-9]{2,10}$", message = "매장코드는 영문 대문자와 숫자로 2~10자로 입력하세요.") String storeCode,
        @NotBlank(message = "매장명을 입력하세요.")
        @Size(max = 50, message = "매장명은 50자 이하로 입력하세요.") String storeName,
        @Size(max = 200, message = "주소는 200자 이하로 입력하세요.") String address,
        @Pattern(regexp = "^[0-9-]{0,20}$", message = "전화번호는 숫자와 하이픈(-)으로 20자 이하로 입력하세요.") String phone) {

    public StoreCreateRequest {
        storeCode = Texts.trim(storeCode);
        storeName = Texts.trim(storeName);
        address = Texts.trimToNull(address);
        phone = Texts.trimToNull(phone);
    }

}
