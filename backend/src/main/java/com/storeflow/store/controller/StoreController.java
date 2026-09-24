package com.storeflow.store.controller;

import com.storeflow.common.code.ActiveStatus;
import com.storeflow.common.response.ApiResponse;
import com.storeflow.common.response.IdResponse;
import com.storeflow.common.response.PageResponse;
import com.storeflow.common.response.StatusResponse;
import com.storeflow.store.dto.StoreCreateRequest;
import com.storeflow.store.dto.StoreDetailResponse;
import com.storeflow.store.dto.StoreOptionResponse;
import com.storeflow.store.dto.StoreResponse;
import com.storeflow.store.dto.StoreSearchRequest;
import com.storeflow.store.dto.StoreUpdateRequest;
import com.storeflow.store.service.StoreService;
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
 * API-STORE-001 ~ 007 (ADMIN 전용)
 */
@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StoreController {

    private final StoreService storeService;

    @GetMapping
    public ApiResponse<PageResponse<StoreResponse>> search(@Valid @ModelAttribute StoreSearchRequest cond) {
        return ApiResponse.ok(storeService.search(cond));
    }

    @GetMapping("/options")
    public ApiResponse<List<StoreOptionResponse>> options() {
        return ApiResponse.ok(storeService.options());
    }

    @GetMapping("/{id}")
    public ApiResponse<StoreDetailResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(storeService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IdResponse> create(@Valid @RequestBody StoreCreateRequest request) {
        return ApiResponse.ok(new IdResponse(storeService.create(request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<IdResponse> update(@PathVariable Long id, @Valid @RequestBody StoreUpdateRequest request) {
        storeService.update(id, request);
        return ApiResponse.ok(new IdResponse(id));
    }

    @PostMapping("/{id}/deactivate")
    public ApiResponse<StatusResponse<ActiveStatus>> deactivate(@PathVariable Long id) {
        return ApiResponse.ok(storeService.changeStatus(id, ActiveStatus.INACTIVE));
    }

    @PostMapping("/{id}/activate")
    public ApiResponse<StatusResponse<ActiveStatus>> activate(@PathVariable Long id) {
        return ApiResponse.ok(storeService.changeStatus(id, ActiveStatus.ACTIVE));
    }

}
