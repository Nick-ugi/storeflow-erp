package com.storeflow.dashboard;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.storeflow.common.code.Role;
import com.storeflow.support.IntegrationTestSupport;
import com.storeflow.support.TestFixtures;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class DashboardApiTest extends IntegrationTestSupport {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final LocalDate today = LocalDate.now(KST);
    private Long gangnam;
    private Long hongdae;
    private Long adminId;
    private int saleSeq;
    private String adminToken;
    private String managerToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        TestFixtures fx = fixtures();
        gangnam = fx.insertStore("GN01", "강남점");
        hongdae = fx.insertStore("HD01", "홍대점");
        Long category = fx.insertCategory("상의");
        Long tshirt = fx.insertProduct(category, "P-0001", "반팔 티셔츠", 12000, 29000);
        Long socks = fx.insertProduct(category, "P-0002", "양말", 1000, 3000);
        Long discontinued = fx.insertProduct(category, "P-0003", "단종 상품", 1000, 2000);
        jdbcTemplate.update("UPDATE products SET status = 'INACTIVE' WHERE id = ?", discontinued);
        fx.insertStock(gangnam, tshirt, 1, 10);
        fx.insertStock(gangnam, socks, 0, 3);
        fx.insertStock(gangnam, discontinued, 2, 5);
        fx.insertStock(hongdae, tshirt, 5, 0);
        fx.insertStock(hongdae, socks, 0, 0);
        fx.insertStock(hongdae, discontinued, 0, 0);
        adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);

        insertSale(gangnam, 10_000, "COMPLETED", today, 10);
        insertSale(gangnam, 5_000, "COMPLETED", today, 11);
        insertSale(gangnam, 7_000, "CANCELLED", today, 12);
        insertSale(gangnam, 3_000, "COMPLETED", today.minusDays(6), 9);
        insertSale(gangnam, 99_999, "COMPLETED", today.minusDays(7), 9);
        insertSale(hongdae, 20_000, "COMPLETED", today, 9);

        adminToken = adminToken();
        managerToken = tokenFor(Role.MANAGER, gangnam);
        userToken = tokenFor(Role.USER, gangnam);
    }

    @Test
    @DisplayName("매출은 '완료' 판매만 집계하고, 최근 7일 추이는 판매 없는 날도 0으로 채운다")
    void salesFigures() throws Exception {
        long monthAmount = 15_000
                + (!today.minusDays(6).isBefore(today.withDayOfMonth(1)) ? 3_000 : 0)
                + (!today.minusDays(7).isBefore(today.withDayOfMonth(1)) ? 99_999 : 0);

        mockMvc.perform(get("/api/v1/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").value(gangnam))
                .andExpect(jsonPath("$.data.todaySales.amount").value(15_000))
                .andExpect(jsonPath("$.data.todaySales.count").value(2))
                .andExpect(jsonPath("$.data.monthSales.amount").value(monthAmount))
                .andExpect(jsonPath("$.data.salesTrend", hasSize(7)))
                .andExpect(jsonPath("$.data.salesTrend[0].date").value(today.minusDays(6).toString()))
                .andExpect(jsonPath("$.data.salesTrend[0].amount").value(3_000))
                .andExpect(jsonPath("$.data.salesTrend[1].amount").value(0))
                .andExpect(jsonPath("$.data.salesTrend[6].date").value(today.toString()))
                .andExpect(jsonPath("$.data.salesTrend[6].amount").value(15_000));
    }

    @Test
    @DisplayName("ADMIN은 전 매장 합계가 기본이고 매장을 선택해서 볼 수 있다")
    void adminScope() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data.storeId", nullValue()))
                .andExpect(jsonPath("$.data.todaySales.amount").value(35_000))
                .andExpect(jsonPath("$.data.todaySales.count").value(3))
                .andExpect(jsonPath("$.data.stockSummary.inStockProductCount").value(2))
                .andExpect(jsonPath("$.data.stockSummary.shortageCount").value(2));
        mockMvc.perform(get("/api/v1/dashboard").param("storeId", String.valueOf(hongdae))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data.todaySales.amount").value(20_000))
                .andExpect(jsonPath("$.data.stockSummary.shortageCount").value(0));
    }

    @Test
    @DisplayName("재고 부족 상품은 부족 수량이 큰 순서이고, 사용 중지 상품은 제외된다")
    void shortageStocks() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(jsonPath("$.data.stockSummary.inStockProductCount").value(2))
                .andExpect(jsonPath("$.data.stockSummary.shortageCount").value(2))
                .andExpect(jsonPath("$.data.shortageStocks", hasSize(2)))
                .andExpect(jsonPath("$.data.shortageStocks[0].productCode").value("P-0001"))
                .andExpect(jsonPath("$.data.shortageStocks[0].shortageQuantity").value(9))
                .andExpect(jsonPath("$.data.shortageStocks[1].productCode").value("P-0002"));
    }

    @Test
    @DisplayName("최근 판매는 최신순 5건이고, 최근 발주는 USER에게 null이다")
    void recentLists() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(jsonPath("$.data.recentSales", hasSize(5)))
                .andExpect(jsonPath("$.data.recentSales[0].totalAmount").value(7_000))
                .andExpect(jsonPath("$.data.recentSales[0].status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.recentPurchaseOrders", hasSize(0)));
        mockMvc.perform(get("/api/v1/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(jsonPath("$.data.recentSales", hasSize(5)))
                .andExpect(jsonPath("$.data.recentPurchaseOrders", nullValue()));
    }

    private void insertSale(Long storeId, long amount, String status, LocalDate date, int hour) {
        OffsetDateTime soldAt = date.atStartOfDay(KST).plusHours(hour).toOffsetDateTime();
        boolean cancelled = "CANCELLED".equals(status);
        jdbcTemplate.update("""
                INSERT INTO sales (sale_number, store_id, total_amount, status, sold_at, created_by, cancelled_at, cancelled_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, "S-TEST-" + (++saleSeq), storeId, amount, status, soldAt, adminId,
                cancelled ? soldAt : null, cancelled ? adminId : null);
    }

}
