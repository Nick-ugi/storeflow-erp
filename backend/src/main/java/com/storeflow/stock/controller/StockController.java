package com.storeflow.stock.controller;

import com.storeflow.common.response.ApiResponse;
import com.storeflow.common.response.PageResponse;
import com.storeflow.common.security.LoginUser;
import com.storeflow.stock.dto.SafetyStockRequest;
import com.storeflow.stock.dto.SafetyStockResponse;
import com.storeflow.stock.dto.StockAdjustmentRequest;
import com.storeflow.stock.dto.StockAdjustmentResponse;
import com.storeflow.stock.dto.StockDetailResponse;
import com.storeflow.stock.dto.StockHistoryResponse;
import com.storeflow.stock.dto.StockHistorySearchRequest;
import com.storeflow.stock.dto.StockResponse;
import com.storeflow.stock.dto.StockSearchRequest;
import com.storeflow.stock.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API-STOCK-001 ~ 005. 조회는 전 역할, 조정 · 안전재고 설정은 ADMIN · MANAGER
 */
@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping
    public ApiResponse<PageResponse<StockResponse>> search(@AuthenticationPrincipal LoginUser loginUser,
                                                           @Valid @ModelAttribute StockSearchRequest cond) {
        return ApiResponse.ok(stockService.search(loginUser, cond));
    }

    @GetMapping("/histories")
    public ApiResponse<PageResponse<StockHistoryResponse>> histories(@AuthenticationPrincipal LoginUser loginUser,
                                                                     @Valid @ModelAttribute StockHistorySearchRequest cond) {
        return ApiResponse.ok(stockService.searchHistories(loginUser, cond));
    }

    @GetMapping("/{productId}")
    public ApiResponse<StockDetailResponse> get(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long productId,
                                                @RequestParam(required = false) Long storeId) {
        return ApiResponse.ok(stockService.get(loginUser, productId, storeId));
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<StockAdjustmentResponse> adjust(@AuthenticationPrincipal LoginUser loginUser,
                                                       @Valid @RequestBody StockAdjustmentRequest request) {
        return ApiResponse.ok(stockService.adjust(loginUser, request));
    }

    @PutMapping("/{productId}/safety-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<SafetyStockResponse> updateSafetyStock(@AuthenticationPrincipal LoginUser loginUser,
                                                              @PathVariable Long productId,
                                                              @RequestParam(required = false) Long storeId,
                                                              @Valid @RequestBody SafetyStockRequest request) {
        return ApiResponse.ok(stockService.updateSafetyStock(loginUser, productId, storeId, request));
    }

}
