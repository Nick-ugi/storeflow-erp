package com.storeflow.dashboard.dto;

import com.storeflow.common.code.SaleStatus;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecentSale {

    private Long id;
    private String saleNumber;
    private OffsetDateTime soldAt;
    private Long totalAmount;
    private SaleStatus status;

}
