package com.storeflow.dashboard.service;

import com.storeflow.common.code.Role;
import com.storeflow.common.config.ClockConfig;
import com.storeflow.common.security.LoginUser;
import com.storeflow.dashboard.dto.DashboardResponse;
import com.storeflow.dashboard.mapper.DashboardMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dashboard (인증 · 시스템 관리 · Dashboard 명세 5장). 날짜는 한국 시간 기준이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int TREND_DAYS = 7;
    private static final int LIST_SIZE = 5;

    private final DashboardMapper dashboardMapper;
    private final Clock clock;

    public DashboardResponse get(LoginUser loginUser, Long requestedStoreId) {
        Long storeId = loginUser.scopeStoreId(requestedStoreId);
        LocalDate today = LocalDate.now(clock);
        OffsetDateTime tomorrowStart = startOf(today.plusDays(1));

        return new DashboardResponse(
                storeId,
                dashboardMapper.sumSales(storeId, startOf(today), tomorrowStart),
                dashboardMapper.sumSales(storeId, startOf(today.withDayOfMonth(1)), tomorrowStart),
                dashboardMapper.dailySales(storeId, today.minusDays(TREND_DAYS - 1L), today),
                dashboardMapper.stockSummary(storeId),
                dashboardMapper.findShortageStocks(storeId, LIST_SIZE),
                dashboardMapper.findRecentSales(storeId, LIST_SIZE),
                // USER는 발주 권한이 없으므로 최근 발주를 보여주지 않는다. (권한표)
                loginUser.role() == Role.USER ? null : dashboardMapper.findRecentPurchaseOrders(storeId, LIST_SIZE));
    }

    private static OffsetDateTime startOf(LocalDate date) {
        return date.atStartOfDay(ClockConfig.BUSINESS_ZONE).toOffsetDateTime();
    }

}
