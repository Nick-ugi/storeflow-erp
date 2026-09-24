package com.storeflow.store.mapper;

import com.storeflow.common.code.ActiveStatus;
import com.storeflow.store.dto.StoreCreateRequest;
import com.storeflow.store.dto.StoreDetailResponse;
import com.storeflow.store.dto.StoreOptionResponse;
import com.storeflow.store.dto.StoreResponse;
import com.storeflow.store.dto.StoreSearchRequest;
import com.storeflow.store.dto.StoreUpdateRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StoreMapper {

    List<StoreResponse> findStores(@Param("cond") StoreSearchRequest cond, @Param("offset") int offset, @Param("limit") int limit);

    long countStores(@Param("cond") StoreSearchRequest cond);

    List<StoreOptionResponse> findOptions();

    StoreDetailResponse findById(Long id);

    ActiveStatus findStatusById(Long id);

    boolean existsByStoreCode(String storeCode);

    boolean existsByStoreName(@Param("storeName") String storeName, @Param("excludeId") Long excludeId);

    Long insert(StoreCreateRequest request);

    int update(@Param("id") Long id, @Param("request") StoreUpdateRequest request);

    int updateStatus(@Param("id") Long id, @Param("status") ActiveStatus status);

}
