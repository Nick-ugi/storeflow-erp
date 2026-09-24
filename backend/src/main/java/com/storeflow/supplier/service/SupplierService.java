package com.storeflow.supplier.service;

import com.storeflow.common.code.ActiveStatus;
import com.storeflow.common.exception.BusinessException;
import com.storeflow.common.exception.ErrorCode;
import com.storeflow.common.response.PageResponse;
import com.storeflow.common.response.StatusResponse;
import com.storeflow.supplier.dto.SupplierDetailResponse;
import com.storeflow.supplier.dto.SupplierOptionResponse;
import com.storeflow.supplier.dto.SupplierRequest;
import com.storeflow.supplier.dto.SupplierResponse;
import com.storeflow.supplier.dto.SupplierSearchRequest;
import com.storeflow.supplier.mapper.SupplierMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공급처 (기준정보 명세 5장)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierService {

    private final SupplierMapper supplierMapper;

    public PageResponse<SupplierResponse> search(SupplierSearchRequest cond) {
        List<SupplierResponse> content = supplierMapper.findSuppliers(cond, cond.offset(), cond.pageSize());
        return PageResponse.of(content, cond, supplierMapper.countSuppliers(cond));
    }

    public List<SupplierOptionResponse> options() {
        return supplierMapper.findOptions();
    }

    public SupplierDetailResponse get(Long id) {
        SupplierDetailResponse supplier = supplierMapper.findById(id);
        if (supplier == null) {
            throw new BusinessException(ErrorCode.SUPPLIER_NOT_FOUND);
        }
        return supplier;
    }

    @Transactional
    public Long create(SupplierRequest request) {
        if (supplierMapper.existsByBusinessNumber(request.businessNumber(), null)) {
            throw new BusinessException(ErrorCode.DUPLICATE_BUSINESS_NUMBER);
        }
        return supplierMapper.insert(request);
    }

    @Transactional
    public void update(Long id, SupplierRequest request) {
        get(id);
        if (supplierMapper.existsByBusinessNumber(request.businessNumber(), id)) {
            throw new BusinessException(ErrorCode.DUPLICATE_BUSINESS_NUMBER);
        }
        supplierMapper.update(id, request);
    }

    /** 사용 중지해도 진행 중인 발주(작성 · 승인)는 그대로 처리할 수 있다. (발주 명세 1.3) */
    @Transactional
    public StatusResponse<ActiveStatus> changeStatus(Long id, ActiveStatus status) {
        get(id);
        supplierMapper.updateStatus(id, status);
        return new StatusResponse<>(id, status);
    }

}
