package com.storeflow.dashboard.controller;

import com.storeflow.common.response.ApiResponse;
import com.storeflow.common.security.LoginUser;
import com.storeflow.dashboard.dto.DashboardResponse;
import com.storeflow.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API-DASH-001. storeId는 ADMIN만 적용되며, 없으면 전 매장 합계
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ApiResponse<DashboardResponse> get(@AuthenticationPrincipal LoginUser loginUser,
                                              @RequestParam(required = false) Long storeId) {
        return ApiResponse.ok(dashboardService.get(loginUser, storeId));
    }

}
