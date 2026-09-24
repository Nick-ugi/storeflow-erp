package com.storeflow.purchase.controller;

import com.storeflow.common.code.PurchaseOrderStatus;
import com.storeflow.common.response.ApiResponse;
import com.storeflow.common.response.IdResponse;
import com.storeflow.common.response.PageResponse;
import com.storeflow.common.response.StatusResponse;
import com.storeflow.common.security.LoginUser;
import com.storeflow.purchase.dto.PurchaseOrderCreateRequest;
import com.storeflow.purchase.dto.PurchaseOrderCreateResponse;
import com.storeflow.purchase.dto.PurchaseOrderDetailResponse;
import com.storeflow.purchase.dto.PurchaseOrderResponse;
import com.storeflow.purchase.dto.PurchaseOrderSearchRequest;
import com.storeflow.purchase.dto.PurchaseOrderUpdateRequest;
import com.storeflow.purchase.service.PurchaseOrderService;
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
 * API-PO-001 ~ 007 (ADMIN · MANAGER)
 */
@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    public ApiResponse<PageResponse<PurchaseOrderResponse>> search(@AuthenticationPrincipal LoginUser loginUser,
                                                                   @Valid @ModelAttribute PurchaseOrderSearchRequest cond) {
        return ApiResponse.ok(purchaseOrderService.search(loginUser, cond));
    }

    @GetMapping("/{id}")
    public ApiResponse<PurchaseOrderDetailResponse> get(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        return ApiResponse.ok(purchaseOrderService.get(loginUser, id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PurchaseOrderCreateResponse> create(@AuthenticationPrincipal LoginUser loginUser,
                                                           @Valid @RequestBody PurchaseOrderCreateRequest request) {
        return ApiResponse.ok(purchaseOrderService.create(loginUser, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<IdResponse> update(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id,
                                          @Valid @RequestBody PurchaseOrderUpdateRequest request) {
        purchaseOrderService.update(loginUser, id, request);
        return ApiResponse.ok(new IdResponse(id));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<StatusResponse<PurchaseOrderStatus>> approve(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        return ApiResponse.ok(purchaseOrderService.approve(loginUser, id));
    }

    @PostMapping("/{id}/receive")
    public ApiResponse<StatusResponse<PurchaseOrderStatus>> receive(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        return ApiResponse.ok(purchaseOrderService.receive(loginUser, id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<StatusResponse<PurchaseOrderStatus>> cancel(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        return ApiResponse.ok(purchaseOrderService.cancel(loginUser, id));
    }

}
