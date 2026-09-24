package com.storeflow.sales.mapper;

import com.storeflow.sales.domain.SaleInsert;
import com.storeflow.sales.domain.SaleItemInsert;
import com.storeflow.sales.domain.SaleQuery;
import com.storeflow.sales.dto.SaleDetailResponse;
import com.storeflow.sales.dto.SaleItemResponse;
import com.storeflow.sales.dto.SaleResponse;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SaleMapper {

    Long insertSale(SaleInsert sale);

    int insertItems(@Param("saleId") Long saleId, @Param("items") List<SaleItemInsert> items);

    List<SaleResponse> findSales(@Param("cond") SaleQuery cond, @Param("offset") int offset, @Param("limit") int limit);

    long countSales(@Param("cond") SaleQuery cond);

    SaleDetailResponse findById(Long id);

    List<SaleItemResponse> findItems(Long saleId);

    /** '완료' 상태일 때만 '취소'로 바꾼다. 변경된 행 수가 0이면 이미 취소된 판매다. */
    int cancel(@Param("id") Long id, @Param("cancelledAt") OffsetDateTime cancelledAt, @Param("cancelledBy") Long cancelledBy);

}
