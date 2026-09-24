package com.storeflow.sales.controller;

import com.storeflow.common.response.ApiResponse;
import com.storeflow.common.response.PageResponse;
import com.storeflow.common.security.LoginUser;
import com.storeflow.sales.dto.SaleCancelResponse;
import com.storeflow.sales.dto.SaleCreateRequest;
import com.storeflow.sales.dto.SaleCreateResponse;
import com.storeflow.sales.dto.SaleDetailResponse;
import com.storeflow.sales.dto.SaleResponse;
import com.storeflow.sales.dto.SaleSearchRequest;
import com.storeflow.sales.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API-SALE-001 ~ 004. 판매 취소는 ADMIN · MANAGER
 */
@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    public ApiResponse<PageResponse<SaleResponse>> search(@AuthenticationPrincipal LoginUser loginUser,
                                                          @Valid @ModelAttribute SaleSearchRequest cond) {
        return ApiResponse.ok(saleService.search(loginUser, cond));
    }

    @GetMapping("/{id}")
    public ApiResponse<SaleDetailResponse> get(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        return ApiResponse.ok(saleService.get(loginUser, id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SaleCreateResponse> create(@AuthenticationPrincipal LoginUser loginUser,
                                                  @Valid @RequestBody SaleCreateRequest request) {
        return ApiResponse.ok(saleService.create(loginUser, request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<SaleCancelResponse> cancel(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        return ApiResponse.ok(saleService.cancel(loginUser, id));
    }

}
