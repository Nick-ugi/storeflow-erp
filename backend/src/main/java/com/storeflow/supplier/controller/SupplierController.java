package com.storeflow.supplier.controller;

import com.storeflow.common.code.ActiveStatus;
import com.storeflow.common.response.ApiResponse;
import com.storeflow.common.response.IdResponse;
import com.storeflow.common.response.PageResponse;
import com.storeflow.common.response.StatusResponse;
import com.storeflow.supplier.dto.SupplierDetailResponse;
import com.storeflow.supplier.dto.SupplierOptionResponse;
import com.storeflow.supplier.dto.SupplierRequest;
import com.storeflow.supplier.dto.SupplierResponse;
import com.storeflow.supplier.dto.SupplierSearchRequest;
import com.storeflow.supplier.service.SupplierService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * API-SUPP-001 ~ 007. 목록은 전 역할, 선택 목록은 ADMIN · MANAGER, 나머지는 ADMIN
 */
@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public ApiResponse<PageResponse<SupplierResponse>> search(@Valid @ModelAttribute SupplierSearchRequest cond) {
        return ApiResponse.ok(supplierService.search(cond));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<List<SupplierOptionResponse>> options() {
        return ApiResponse.ok(supplierService.options());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SupplierDetailResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(supplierService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<IdResponse> create(@Valid @RequestBody SupplierRequest request) {
        return ApiResponse.ok(new IdResponse(supplierService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<IdResponse> update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        supplierService.update(id, request);
        return ApiResponse.ok(new IdResponse(id));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StatusResponse<ActiveStatus>> deactivate(@PathVariable Long id) {
        return ApiResponse.ok(supplierService.changeStatus(id, ActiveStatus.INACTIVE));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StatusResponse<ActiveStatus>> activate(@PathVariable Long id) {
        return ApiResponse.ok(supplierService.changeStatus(id, ActiveStatus.ACTIVE));
    }

}
