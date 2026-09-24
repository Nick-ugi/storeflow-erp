package com.storeflow.purchase.mapper;

import com.storeflow.purchase.domain.LockedPurchaseOrder;
import com.storeflow.purchase.domain.PurchaseOrderInsert;
import com.storeflow.purchase.domain.PurchaseOrderItemInsert;
import com.storeflow.purchase.domain.PurchaseOrderQuery;
import com.storeflow.purchase.dto.PurchaseOrderDetailResponse;
import com.storeflow.purchase.dto.PurchaseOrderItemResponse;
import com.storeflow.purchase.dto.PurchaseOrderResponse;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PurchaseOrderMapper {

    Long insertOrder(PurchaseOrderInsert order);

    int insertItems(@Param("purchaseOrderId") Long purchaseOrderId, @Param("items") List<PurchaseOrderItemInsert> items);

    int deleteItems(Long purchaseOrderId);

    int updateOrder(@Param("id") Long id, @Param("supplierId") Long supplierId, @Param("totalAmount") long totalAmount,
                    @Param("updatedBy") Long updatedBy);

    List<PurchaseOrderResponse> findOrders(@Param("cond") PurchaseOrderQuery cond, @Param("offset") int offset, @Param("limit") int limit);

    long countOrders(@Param("cond") PurchaseOrderQuery cond);

    PurchaseOrderDetailResponse findById(Long id);

    List<PurchaseOrderItemResponse> findItems(Long purchaseOrderId);

    /** 발주 행을 잠그고 현재 상태를 읽는다. 수정 · 상태 변경이 동시에 들어와도 순서대로 처리된다. */
    LockedPurchaseOrder lockById(Long id);

    int approve(@Param("id") Long id, @Param("at") OffsetDateTime at, @Param("by") Long by);

    int complete(@Param("id") Long id, @Param("at") OffsetDateTime at, @Param("by") Long by);

    int cancel(@Param("id") Long id, @Param("at") OffsetDateTime at, @Param("by") Long by);

}
