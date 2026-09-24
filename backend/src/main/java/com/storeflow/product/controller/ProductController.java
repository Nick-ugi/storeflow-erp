package com.storeflow.product.controller;

import com.storeflow.common.code.ActiveStatus;
import com.storeflow.common.response.ApiResponse;
import com.storeflow.common.response.IdResponse;
import com.storeflow.common.response.PageResponse;
import com.storeflow.common.response.StatusResponse;
import com.storeflow.common.security.LoginUser;
import com.storeflow.product.dto.ProductCreateRequest;
import com.storeflow.product.dto.ProductDetailResponse;
import com.storeflow.product.dto.ProductResponse;
import com.storeflow.product.dto.ProductSearchRequest;
import com.storeflow.product.dto.ProductUpdateRequest;
import com.storeflow.product.service.ProductService;
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
 * API-PROD-001 ~ 006. 조회는 전 역할, 등록 · 수정 · 사용 중지는 ADMIN
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> search(@AuthenticationPrincipal LoginUser loginUser,
                                                             @Valid @ModelAttribute ProductSearchRequest cond) {
        return ApiResponse.ok(productService.search(loginUser, cond));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(productService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<IdResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        return ApiResponse.ok(new IdResponse(productService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<IdResponse> update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        productService.update(id, request);
        return ApiResponse.ok(new IdResponse(id));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StatusResponse<ActiveStatus>> deactivate(@PathVariable Long id) {
        return ApiResponse.ok(productService.changeStatus(id, ActiveStatus.INACTIVE));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StatusResponse<ActiveStatus>> activate(@PathVariable Long id) {
        return ApiResponse.ok(productService.changeStatus(id, ActiveStatus.ACTIVE));
    }

}
