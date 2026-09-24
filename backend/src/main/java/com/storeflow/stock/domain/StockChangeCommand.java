package com.storeflow.stock.domain;

import com.storeflow.common.code.ReferenceType;
import com.storeflow.common.code.StockHistoryType;
import java.util.List;

/**
 * 재고 변경 공통 절차의 입력 (판매 · 재고 명세 1.3).
 * 변동 유형과 관련 문서 조합은 DB 제약(ck_stock_histories_type)과 같도록 아래 생성 메서드로만 만든다.
 */
public record StockChangeCommand(
        Long storeId,
        List<StockChange> changes,
        StockHistoryType type,
        ReferenceType referenceType,
        Long referenceId,
        String reason,
        Long processedBy) {

    public static StockChangeCommand sale(Long storeId, List<StockChange> changes, Long saleId, Long processedBy) {
        return new StockChangeCommand(storeId, changes, StockHistoryType.SALE, ReferenceType.SALE, saleId, null, processedBy);
    }

    public static StockChangeCommand saleCancel(Long storeId, List<StockChange> changes, Long saleId, Long processedBy) {
        return new StockChangeCommand(storeId, changes, StockHistoryType.SALE_CANCEL, ReferenceType.SALE, saleId, null, processedBy);
    }

    public static StockChangeCommand purchase(Long storeId, List<StockChange> changes, Long purchaseOrderId, Long processedBy) {
        return new StockChangeCommand(storeId, changes, StockHistoryType.PURCHASE, ReferenceType.PURCHASE_ORDER, purchaseOrderId, null, processedBy);
    }

    public static StockChangeCommand adjustment(Long storeId, StockChange change, String reason, Long processedBy) {
        return new StockChangeCommand(storeId, List.of(change), StockHistoryType.ADJUSTMENT, null, null, reason, processedBy);
    }

}
