package com.storeflow.auth.controller;

import com.storeflow.auth.dto.LoginRequest;
import com.storeflow.auth.dto.LoginResponse;
import com.storeflow.auth.dto.PasswordChangeRequest;
import com.storeflow.auth.dto.UserInfoResponse;
import com.storeflow.auth.service.AuthService;
import com.storeflow.common.response.ApiResponse;
import com.storeflow.common.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API-AUTH-001 ~ 004
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    /** 서버는 상태를 갖지 않으므로 처리할 것이 없다. 클라이언트가 토큰을 삭제한다. */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> me(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResponse.ok(UserInfoResponse.from(loginUser));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal LoginUser loginUser,
                                            @Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(loginUser, request);
        return ApiResponse.ok();
    }

}
