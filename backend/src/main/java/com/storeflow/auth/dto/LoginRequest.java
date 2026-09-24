package com.storeflow.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "아이디와 비밀번호를 입력하세요.") String username,
        @NotBlank(message = "아이디와 비밀번호를 입력하세요.") String password) {
}
