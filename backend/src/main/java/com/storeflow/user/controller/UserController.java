package com.storeflow.user.controller;

import com.storeflow.common.code.ActiveStatus;
import com.storeflow.common.response.ApiResponse;
import com.storeflow.common.response.IdResponse;
import com.storeflow.common.response.PageResponse;
import com.storeflow.common.response.StatusResponse;
import com.storeflow.common.security.LoginUser;
import com.storeflow.user.dto.PasswordResetRequest;
import com.storeflow.user.dto.UserCreateRequest;
import com.storeflow.user.dto.UserDetailResponse;
import com.storeflow.user.dto.UserResponse;
import com.storeflow.user.dto.UserSearchRequest;
import com.storeflow.user.dto.UserUpdateRequest;
import com.storeflow.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API-USER-001 ~ 007 (ADMIN 전용)
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> search(@Valid @ModelAttribute UserSearchRequest cond) {
        return ApiResponse.ok(userService.search(cond));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserDetailResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(userService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IdResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok(new IdResponse(userService.create(request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<IdResponse> update(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id,
                                          @Valid @RequestBody UserUpdateRequest request) {
        userService.update(loginUser, id, request);
        return ApiResponse.ok(new IdResponse(id));
    }

    @PostMapping("/{id}/deactivate")
    public ApiResponse<StatusResponse<ActiveStatus>> deactivate(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        return ApiResponse.ok(userService.deactivate(loginUser, id));
    }

    @PostMapping("/{id}/activate")
    public ApiResponse<StatusResponse<ActiveStatus>> activate(@PathVariable Long id) {
        return ApiResponse.ok(userService.activate(id));
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(id, request);
        return ApiResponse.ok();
    }

}
