package com.storeflow.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 매출 금액 · 판매 건수. '완료' 판매만 집계한다. (BR-060)
 */
@Getter
@Setter
public class SalesSummary {

    private Long amount;
    private Long count;

}
