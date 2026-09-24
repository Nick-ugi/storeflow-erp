package com.storeflow.common.code;

/**
 * 재고 변동 유형 (stock_histories.type) — 코드 정의서 2.5
 */
public enum StockHistoryType {

    /** 판매 (−), 관련 문서: 판매 */
    SALE,
    /** 판매 취소 (+), 관련 문서: 판매 */
    SALE_CANCEL,
    /** 입고 (+), 관련 문서: 발주 */
    PURCHASE,
    /** 조정 (±), 관련 문서 없음, 사유 필수 */
    ADJUSTMENT

}
