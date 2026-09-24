package com.storeflow.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.storeflow.common.code.Role;
import com.storeflow.support.IntegrationTestSupport;
import com.storeflow.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class StockApiTest extends IntegrationTestSupport {

    private TestFixtures fx;
    private Long gangnam;
    private Long hongdae;
    private Long tshirt;
    private Long socks;
    private Long discontinued;
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
        discontinued = fx.insertProduct(category, "P-0003", "단종 상품", 1000, 2000);
        jdbcTemplate.update("UPDATE products SET status = 'INACTIVE' WHERE id = ?", discontinued);
        fx.insertStock(gangnam, tshirt, 10, 12);
        fx.insertStock(gangnam, socks, 5, 0);
        fx.insertStock(gangnam, discontinued, 1, 5);
        fx.insertStock(hongdae, tshirt, 3, 10);
        fx.insertStock(hongdae, socks, 0, 0);
        fx.insertStock(hongdae, discontinued, 0, 0);
        adminToken = adminToken();
        managerToken = tokenFor(Role.MANAGER, gangnam);
        userToken = tokenFor(Role.USER, gangnam);
    }

    @Test
    @DisplayName("부족 재고는 사용 중인 상품의 현재 수량 < 안전재고이며, 사용 중지 상품과 안전재고 0은 제외된다")
    void shortage() throws Exception {
        mockMvc.perform(get("/api/v1/stocks").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.content[0].productCode").value("P-0001"))
                .andExpect(jsonPath("$.data.content[0].shortage").value(true))
                .andExpect(jsonPath("$.data.content[1].shortage").value(false))
                .andExpect(jsonPath("$.data.content[2].productStatus").value("INACTIVE"))
                .andExpect(jsonPath("$.data.content[2].shortage").value(false));

        mockMvc.perform(get("/api/v1/stocks").param("shortageOnly", "true").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].storeName").value("강남점"))
                .andExpect(jsonPath("$.data.content[1].storeName").value("홍대점"));
    }

    @Test
    @DisplayName("재고 조정: 사유와 함께 증가 · 감소하고 실제 변동 전 · 후 수량을 돌려준다")
    void adjust() throws Exception {
        adjust(managerToken, null, tshirt, -3, "파손 3개 폐기")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.beforeQuantity").value(10))
                .andExpect(jsonPath("$.data.afterQuantity").value(7));
        adjust(managerToken, null, tshirt, 5, "실사 결과 보정")
                .andExpect(jsonPath("$.data.afterQuantity").value(12));

        assertThat(fx.stockQuantity(gangnam, tshirt)).isEqualTo(12);
        assertThat(fx.count("SELECT count(*) FROM stock_histories WHERE type = 'ADJUSTMENT' AND reason IS NOT NULL")).isEqualTo(2);
    }

    @Test
    @DisplayName("재고 조정 검증: 0 미만 결과는 409, 수량 0 · 사유 없음은 400, USER는 403, ADMIN은 매장 필수")
    void adjustValidation() throws Exception {
        adjust(managerToken, null, socks, -6, "분실")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.error.message").value("조정 후 재고는 0보다 작을 수 없습니다. (현재 재고 5개)"));
        adjust(managerToken, null, socks, 0, "사유")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("조정 수량은 1 이상 9,999 이하로 입력하세요."));
        adjust(managerToken, null, socks, 1, "   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("조정 사유를 입력하세요."));
        adjust(userToken, null, socks, 1, "사유").andExpect(status().isForbidden());
        adjust(adminToken, null, socks, 1, "사유")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("매장을 선택하세요."));
        adjust(adminToken, hongdae, socks, 4, "입고 누락 보정").andExpect(status().isOk());
        assertThat(fx.stockQuantity(hongdae, socks)).isEqualTo(4);
    }

    @Test
    @DisplayName("재고 상세: 최근 변동 이력은 최신순 최대 10건")
    void detailWithRecentHistories() throws Exception {
        for (int i = 1; i <= 12; i++) {
            adjust(managerToken, null, socks, 1, "보정 " + i).andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/stocks/{productId}", socks).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(17))
                .andExpect(jsonPath("$.data.recentHistories", hasSize(10)))
                .andExpect(jsonPath("$.data.recentHistories[0].reason").value("보정 12"))
                .andExpect(jsonPath("$.data.recentHistories[0].afterQuantity").value(17));

        mockMvc.perform(get("/api/v1/stocks/{productId}", socks).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/stocks/{productId}", socks).param("storeId", String.valueOf(hongdae))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data.storeName").value("홍대점"));
    }

    @Test
    @DisplayName("안전재고 설정은 ADMIN · MANAGER, 범위 0~99,999, 재고 이력은 남기지 않는다")
    void safetyStock() throws Exception {
        mockMvc.perform(put("/api/v1/stocks/{productId}/safety-stock", socks)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"safetyStock\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.safetyStock").value(20));
        mockMvc.perform(put("/api/v1/stocks/{productId}/safety-stock", socks)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"safetyStock\":100000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("안전재고는 0 이상 99,999 이하로 입력하세요."));
        mockMvc.perform(put("/api/v1/stocks/{productId}/safety-stock", socks)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"safetyStock\":1}"))
                .andExpect(status().isForbidden());

        assertThat(fx.count("SELECT count(*) FROM stock_histories")).isZero();
        mockMvc.perform(get("/api/v1/stocks/{productId}", socks).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(jsonPath("$.data.shortage").value(true));
    }

    @Test
    @DisplayName("재고 이력: 판매번호가 관련 번호로 표시되고, 유형 · 상품 조건으로 거를 수 있다")
    void histories() throws Exception {
        mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":%d,\"quantity\":2}]}".formatted(tshirt)))
                .andExpect(status().isCreated());
        adjust(managerToken, null, socks, -1, "파손").andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/stocks/histories").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].type").value("ADJUSTMENT"));
        mockMvc.perform(get("/api/v1/stocks/histories").param("type", "SALE").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].referenceType").value("SALE"))
                .andExpect(jsonPath("$.data.content[0].referenceNumber").value(org.hamcrest.Matchers.startsWith("S-GN01-")))
                .andExpect(jsonPath("$.data.content[0].quantity").value(-2));
        mockMvc.perform(get("/api/v1/stocks/histories").param("productId", String.valueOf(socks))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].reason").value("파손"));
    }

    private ResultActions adjust(String token, Long storeId, Long productId, int quantity, String reason) throws Exception {
        return mockMvc.perform(post("/api/v1/stocks/adjustments")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"storeId\":%s,\"productId\":%d,\"quantity\":%d,\"reason\":\"%s\"}".formatted(storeId, productId, quantity, reason)));
    }

}
