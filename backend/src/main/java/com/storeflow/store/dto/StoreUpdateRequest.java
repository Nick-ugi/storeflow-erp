package com.storeflow.store.dto;

import com.storeflow.common.util.Texts;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 매장코드는 등록 후 변경할 수 없으므로 받지 않는다.
 */
public record StoreUpdateRequest(
        @NotBlank(message = "매장명을 입력하세요.")
        @Size(max = 50, message = "매장명은 50자 이하로 입력하세요.") String storeName,
        @Size(max = 200, message = "주소는 200자 이하로 입력하세요.") String address,
        @Pattern(regexp = "^[0-9-]{0,20}$", message = "전화번호는 숫자와 하이픈(-)으로 20자 이하로 입력하세요.") String phone) {

    public StoreUpdateRequest {
        storeName = Texts.trim(storeName);
        address = Texts.trimToNull(address);
        phone = Texts.trimToNull(phone);
    }

}
