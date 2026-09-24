package com.storeflow.dashboard.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DailySales {

    private LocalDate date;
    private Long amount;

}
