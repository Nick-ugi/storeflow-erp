package com.storeflow.user.dto;

import com.storeflow.common.code.Role;
import com.storeflow.common.util.Texts;
import com.storeflow.common.validation.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank(message = "아이디를 입력하세요.")
        @Pattern(regexp = "^[a-z0-9]{4,20}$", message = "아이디는 영문 소문자와 숫자로 4~20자로 입력하세요.") String username,
        @NotBlank(message = "초기 비밀번호를 입력하세요.")
        @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.MESSAGE) String password,
        @NotBlank(message = "이름을 입력하세요.")
        @Size(max = 50, message = "이름은 50자 이하로 입력하세요.") String name,
        @NotNull(message = "역할을 선택하세요.") Role role,
        Long storeId) {

    public UserCreateRequest {
        username = Texts.trim(username);
        name = Texts.trim(name);
    }

}
