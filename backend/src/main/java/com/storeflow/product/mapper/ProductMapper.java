package com.storeflow.product.mapper;

import com.storeflow.common.code.ActiveStatus;
import com.storeflow.product.domain.ProductSnapshot;
import com.storeflow.product.dto.ProductCreateRequest;
import com.storeflow.product.dto.ProductDetailResponse;
import com.storeflow.product.dto.ProductResponse;
import com.storeflow.product.dto.ProductSearchRequest;
import com.storeflow.product.dto.ProductUpdateRequest;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductMapper {

    /**
     * @param stockStoreId 재고 수량 기준 매장. null이면 전 매장 합계
     */
    List<ProductResponse> findProducts(@Param("cond") ProductSearchRequest cond, @Param("stockStoreId") Long stockStoreId,
                                       @Param("offset") int offset, @Param("limit") int limit);

    long countProducts(@Param("cond") ProductSearchRequest cond);

    ProductDetailResponse findById(Long id);

    List<ProductSnapshot> findSnapshots(@Param("ids") Collection<Long> ids);

    boolean existsByProductCode(String productCode);

    Long insert(ProductCreateRequest request);

    int update(@Param("id") Long id, @Param("request") ProductUpdateRequest request);

    int updateStatus(@Param("id") Long id, @Param("status") ActiveStatus status);

}
