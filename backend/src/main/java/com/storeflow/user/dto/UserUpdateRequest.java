package com.storeflow.user.dto;

import com.storeflow.common.code.Role;
import com.storeflow.common.util.Texts;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 아이디 · 비밀번호는 수정하지 않는다. (비밀번호는 초기화 API로 변경)
 */
public record UserUpdateRequest(
        @NotBlank(message = "이름을 입력하세요.")
        @Size(max = 50, message = "이름은 50자 이하로 입력하세요.") String name,
        @NotNull(message = "역할을 선택하세요.") Role role,
        Long storeId) {

    public UserUpdateRequest {
        name = Texts.trim(name);
    }

}
