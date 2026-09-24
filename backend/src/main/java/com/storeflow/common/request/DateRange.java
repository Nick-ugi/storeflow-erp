package com.storeflow.common.request;

import com.storeflow.common.config.ClockConfig;
import com.storeflow.common.exception.BusinessException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 목록의 기간 조건. 한국 날짜 기준 [시작일 00:00, 종료일 다음날 00:00) 범위로 바꾼다.
 */
public record DateRange(OffsetDateTime from, OffsetDateTime to) {

    /**
     * @param defaultDays 기간을 지정하지 않았을 때 오늘을 포함해 조회할 일 수
     */
    public static DateRange resolve(LocalDate startDate, LocalDate endDate, int defaultDays, Clock clock) {
        LocalDate end = endDate != null ? endDate : LocalDate.now(clock);
        LocalDate start = startDate != null ? startDate : end.minusDays(defaultDays - 1L);
        if (start.isAfter(end)) {
            throw BusinessException.invalidField("startDate", "시작일은 종료일보다 늦을 수 없습니다.");
        }
        if (start.isBefore(end.minusYears(1))) {
            throw BusinessException.invalidField("startDate", "조회 기간은 최대 1년입니다.");
        }
        return new DateRange(
                start.atStartOfDay(ClockConfig.BUSINESS_ZONE).toOffsetDateTime(),
                end.plusDays(1).atStartOfDay(ClockConfig.BUSINESS_ZONE).toOffsetDateTime());
    }

}
