package com.storeflow.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.storeflow.common.code.Role;
import com.storeflow.support.IntegrationTestSupport;
import com.storeflow.support.TestFixtures;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class SaleApiTest extends IntegrationTestSupport {

    private TestFixtures fx;
    private Long gangnam;
    private Long hongdae;
    private Long tshirt;
    private Long socks;
    private String adminToken;
    private String managerToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        fx = fixtures();
        gangnam = fx.insertStore("GN01", "강남점");
        hongdae = fx.insertStore("HD01", "홍대점");
        Long category = fx.insertCategory("상의");
        tshirt = fx.insertProduct(category, "P-0001", "반팔 티셔츠", 12000, 29000);
        socks = fx.insertProduct(category, "P-0002", "양말", 1000, 3000);
        for (Long store : new Long[] {gangnam, hongdae}) {
            fx.insertStock(store, tshirt, 10, 0);
            fx.insertStock(store, socks, 5, 0);
        }
        adminToken = adminToken();
        managerToken = tokenFor(Role.MANAGER, gangnam);
        userToken = tokenFor(Role.USER, gangnam);
    }

    @Test
    @DisplayName("판매 등록: 서버의 판매가로 금액을 계산하고, 재고 차감과 재고 이력이 함께 기록된다")
    void createSale() throws Exception {
        String body = sell(userToken, null, item(tshirt, 2), item(socks, 3))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.saleNumber", matchesPattern("S-GN01-\\d{8}-\\d{6}")))
                .andReturn().getResponse().getContentAsString();
        Long saleId = ((Number) JsonPath.read(body, "$.data.id")).longValue();

        assertThat(jdbcTemplate.queryForObject("SELECT total_amount FROM sales WHERE id = ?", Long.class, saleId))
                .isEqualTo(2 * 29000 + 3 * 3000);
        assertThat(fx.stockQuantity(gangnam, tshirt)).isEqualTo(8);
        assertThat(fx.stockQuantity(gangnam, socks)).isEqualTo(2);
        assertThat(fx.stockQuantity(hongdae, tshirt)).as("다른 매장 재고는 그대로").isEqualTo(10);

        var histories = jdbcTemplate.queryForList("""
                SELECT type, quantity, before_quantity, after_quantity, reference_type, reference_id
                  FROM stock_histories WHERE reference_id = ? ORDER BY product_id
                """, saleId);
        assertThat(histories).hasSize(2);
        assertThat(histories.getFirst()).containsEntry("type", "SALE").containsEntry("quantity", -2)
                .containsEntry("before_quantity", 10).containsEntry("after_quantity", 8)
                .containsEntry("reference_type", "SALE");
    }

    @Test
    @DisplayName("재고보다 많이 팔면 409 INSUFFICIENT_STOCK과 상품명 · 현재 재고 · 판매 수량을 알려준다")
    void insufficientStock() throws Exception {
        sell(userToken, null, item(tshirt, 2), item(socks, 6))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.error.message").value("재고가 부족합니다: 양말 (현재 재고 5개, 판매 수량 6개)"));
    }

    @Test
    @DisplayName("입력 검증: 상품 없음, 같은 상품 중복, 수량 범위, 50개 초과")
    void validation() throws Exception {
        sellRaw(userToken, "{\"items\":[]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("판매할 상품을 1개 이상 추가하세요."));
        sell(userToken, null, item(tshirt, 1), item(tshirt, 2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("같은 상품이 중복되었습니다."));
        sell(userToken, null, item(tshirt, 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("items[0].quantity"))
                .andExpect(jsonPath("$.error.message").value("수량은 1 이상 9,999 이하로 입력하세요."));

        String[] many = new String[51];
        for (int i = 0; i < many.length; i++) {
            many[i] = item(1000L + i, 1);
        }
        sell(userToken, null, many)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("한 번에 최대 50개 상품까지 판매할 수 있습니다."));
    }

    @Test
    @DisplayName("사용 중지 상품 · 매장은 판매할 수 없고, ADMIN은 매장을 지정해야 한다")
    void unavailableProductStoreAndAdminStore() throws Exception {
        jdbcTemplate.update("UPDATE products SET status = 'INACTIVE' WHERE id = ?", tshirt);
        sell(userToken, null, item(tshirt, 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.error.message").value("판매할 수 없는 상품이 포함되어 있습니다: 반팔 티셔츠"));

        jdbcTemplate.update("UPDATE stores SET status = 'INACTIVE' WHERE id = ?", hongdae);
        sell(adminToken, hongdae, item(socks, 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("STORE_INACTIVE"));

        sell(adminToken, null, item(socks, 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("판매할 매장을 선택하세요."));
    }

    @Test
    @DisplayName("MANAGER가 다른 매장을 지정해도 소속 매장 판매로 처리된다")
    void managerStoreIsForced() throws Exception {
        String body = sell(managerToken, hongdae, item(socks, 1)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long saleId = ((Number) JsonPath.read(body, "$.data.id")).longValue();

        assertThat(jdbcTemplate.queryForObject("SELECT store_id FROM sales WHERE id = ?", Long.class, saleId)).isEqualTo(gangnam);
        assertThat(fx.stockQuantity(hongdae, socks)).isEqualTo(5);
    }

    @Test
    @DisplayName("판매 후 판매가를 바꿔도 판매 상세의 단가와 합계는 판매 시점 그대로다 (BR-041)")
    void priceSnapshot() throws Exception {
        Long saleId = createSale(userToken, item(tshirt, 2));
        jdbcTemplate.update("UPDATE products SET sale_price = 35000 WHERE id = ?", tshirt);

        mockMvc.perform(get("/api/v1/sales/{id}", saleId).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(29000))
                .andExpect(jsonPath("$.data.items[0].totalPrice").value(58000))
                .andExpect(jsonPath("$.data.totalAmount").value(58000))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.cancelledAt").doesNotExist());
    }

    @Test
    @DisplayName("판매 취소: 재고가 복원되고 판매 취소 이력이 남으며, 다시 취소하면 409")
    void cancelSale() throws Exception {
        Long saleId = createSale(userToken, item(tshirt, 2), item(socks, 1));

        mockMvc.perform(post("/api/v1/sales/{id}/cancel", saleId).header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelledAt", matchesPattern(".*\\+09:00")));

        assertThat(fx.stockQuantity(gangnam, tshirt)).isEqualTo(10);
        assertThat(fx.stockQuantity(gangnam, socks)).isEqualTo(5);
        assertThat(fx.count("SELECT count(*) FROM stock_histories WHERE type = 'SALE_CANCEL' AND reference_id = ?", saleId))
                .isEqualTo(2);
        mockMvc.perform(get("/api/v1/sales/{id}", saleId).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelledByName").exists());

        mockMvc.perform(post("/api/v1/sales/{id}/cancel", saleId).header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SALE_ALREADY_CANCELLED"));
    }

    @Test
    @DisplayName("판매 취소 권한: USER는 403, 다른 매장 MANAGER도 403")
    void cancelPermission() throws Exception {
        Long saleId = createSale(userToken, item(tshirt, 1));
        String otherManager = tokenFor(Role.MANAGER, hongdae);

        mockMvc.perform(post("/api/v1/sales/{id}/cancel", saleId).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/sales/{id}/cancel", saleId).header(HttpHeaders.AUTHORIZATION, bearer(otherManager)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/sales/{id}", saleId).header(HttpHeaders.AUTHORIZATION, bearer(otherManager)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/sales/{id}", 999_999).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SALE_NOT_FOUND"));
    }

    @Test
    @DisplayName("판매 목록: 매장 범위 · 상태 · 판매번호 조건과 품목 수")
    void searchSales() throws Exception {
        Long first = createSale(userToken, item(tshirt, 1), item(socks, 1));
        createSale(userToken, item(socks, 1));
        sell(adminToken, hongdae, item(tshirt, 1)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/sales/{id}/cancel", first).header(HttpHeaders.AUTHORIZATION, bearer(managerToken)));

        mockMvc.perform(get("/api/v1/sales").header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(jsonPath("$.data.totalElements").value(2));
        mockMvc.perform(get("/api/v1/sales").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data.totalElements").value(3));
        mockMvc.perform(get("/api/v1/sales").param("status", "CANCELLED").header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].itemCount").value(2));

        String saleNumber = jdbcTemplate.queryForObject("SELECT sale_number FROM sales WHERE id = ?", String.class, first);
        mockMvc.perform(get("/api/v1/sales").param("saleNumber", saleNumber.substring(saleNumber.length() - 6))
                        .param("startDate", "2020-01-01").param("endDate", "2020-01-02")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("판매 목록 기간: 시작일이 종료일보다 늦거나 1년을 넘으면 400")
    void dateRangeValidation() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        mockMvc.perform(get("/api/v1/sales").param("startDate", today.toString()).param("endDate", today.minusDays(1).toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("시작일은 종료일보다 늦을 수 없습니다."));
        mockMvc.perform(get("/api/v1/sales").param("startDate", today.minusYears(1).minusDays(1).toString())
                        .param("endDate", today.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("조회 기간은 최대 1년입니다."));
    }

    private Long createSale(String token, String... items) throws Exception {
        String body = sell(token, null, items).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    private ResultActions sell(String token, Long storeId, String... items) throws Exception {
        return sellRaw(token, "{\"storeId\":%s,\"items\":[%s]}".formatted(storeId, String.join(",", items)));
    }

    private ResultActions sellRaw(String token, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/sales")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String item(Long productId, int quantity) {
        return "{\"productId\":%d,\"quantity\":%d}".formatted(productId, quantity);
    }

}
