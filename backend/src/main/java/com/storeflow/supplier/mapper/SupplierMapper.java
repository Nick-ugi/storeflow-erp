package com.storeflow.supplier.mapper;

import com.storeflow.common.code.ActiveStatus;
import com.storeflow.supplier.dto.SupplierDetailResponse;
import com.storeflow.supplier.dto.SupplierOptionResponse;
import com.storeflow.supplier.dto.SupplierRequest;
import com.storeflow.supplier.dto.SupplierResponse;
import com.storeflow.supplier.dto.SupplierSearchRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SupplierMapper {

    List<SupplierResponse> findSuppliers(@Param("cond") SupplierSearchRequest cond, @Param("offset") int offset, @Param("limit") int limit);

    long countSuppliers(@Param("cond") SupplierSearchRequest cond);

    List<SupplierOptionResponse> findOptions();

    SupplierDetailResponse findById(Long id);

    boolean existsByBusinessNumber(@Param("businessNumber") String businessNumber, @Param("excludeId") Long excludeId);

    Long insert(SupplierRequest request);

    int update(@Param("id") Long id, @Param("request") SupplierRequest request);

    int updateStatus(@Param("id") Long id, @Param("status") ActiveStatus status);

}
