package com.storeflow.store.service;

import com.storeflow.common.code.ActiveStatus;
import com.storeflow.common.exception.BusinessException;
import com.storeflow.common.exception.ErrorCode;
import com.storeflow.common.response.PageResponse;
import com.storeflow.common.response.StatusResponse;
import com.storeflow.stock.service.StockRowInitializer;
import com.storeflow.store.dto.StoreCreateRequest;
import com.storeflow.store.dto.StoreDetailResponse;
import com.storeflow.store.dto.StoreOptionResponse;
import com.storeflow.store.dto.StoreResponse;
import com.storeflow.store.dto.StoreSearchRequest;
import com.storeflow.store.dto.StoreUpdateRequest;
import com.storeflow.store.mapper.StoreMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreMapper storeMapper;
    private final StockRowInitializer stockRowInitializer;

    public PageResponse<StoreResponse> search(StoreSearchRequest cond) {
        List<StoreResponse> content = storeMapper.findStores(cond, cond.offset(), cond.pageSize());
        return PageResponse.of(content, cond, storeMapper.countStores(cond));
    }

    public List<StoreOptionResponse> options() {
        return storeMapper.findOptions();
    }

    public StoreDetailResponse get(Long id) {
        StoreDetailResponse store = storeMapper.findById(id);
        if (store == null) {
            throw new BusinessException(ErrorCode.STORE_NOT_FOUND);
        }
        return store;
    }

    /**
     * 판매 · 발주 등록, 사용자 소속 매장 지정처럼 사용 중인 매장만 허용하는 곳에서 사용한다. (BR-014)
     */
    public void requireActive(Long storeId, String inactiveMessage) {
        ActiveStatus status = storeMapper.findStatusById(storeId);
        if (status == null) {
            throw new BusinessException(ErrorCode.STORE_NOT_FOUND);
        }
        if (status != ActiveStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.STORE_INACTIVE, inactiveMessage);
        }
    }

    /**
     * 매장을 등록하고 모든 상품의 재고 행(수량 0)을 함께 만든다. (시스템 관리 명세 3.2)
     */
    @Transactional
    public Long create(StoreCreateRequest request) {
        stockRowInitializer.lockRegistration();
        if (storeMapper.existsByStoreCode(request.storeCode())) {
            throw new BusinessException(ErrorCode.DUPLICATE_STORE_CODE);
        }
        if (storeMapper.existsByStoreName(request.storeName(), null)) {
            throw new BusinessException(ErrorCode.DUPLICATE_STORE_NAME);
        }
        Long storeId = storeMapper.insert(request);
        stockRowInitializer.createForStore(storeId);
        return storeId;
    }

    @Transactional
    public void update(Long id, StoreUpdateRequest request) {
        get(id);
        if (storeMapper.existsByStoreName(request.storeName(), id)) {
            throw new BusinessException(ErrorCode.DUPLICATE_STORE_NAME);
        }
        storeMapper.update(id, request);
    }

    @Transactional
    public StatusResponse<ActiveStatus> changeStatus(Long id, ActiveStatus status) {
        get(id);
        storeMapper.updateStatus(id, status);
        return new StatusResponse<>(id, status);
    }

}
