package com.storeflow.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.storeflow.common.code.Role;
import com.storeflow.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class StoreApiTest extends IntegrationTestSupport {

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = adminToken();
    }

    @Test
    @DisplayName("매장을 등록하면 기존 모든 상품의 재고 행이 수량 0으로 함께 생성된다")
    void createStoreCreatesStockRows() throws Exception {
        Long categoryId = insertCategory("상의");
        Long product1 = insertProduct(categoryId, "P-0001", "반팔 티셔츠", 12000, 29000);
        Long product2 = insertProduct(categoryId, "P-0002", "셔츠", 20000, 49000);

        String body = mockMvc.perform(post("/api/v1/stores")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("storeCode", "GN01", "storeName", "  강남점  ", "address", "서울시 강남구", "phone", "02-555-0101")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long storeId = ((Number) JsonPath.read(body, "$.data.id")).longValue();

        assertThat(countStocks(storeId, product1)).isEqualTo(1);
        assertThat(countStocks(storeId, product2)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT store_name FROM stores WHERE id = ?", String.class, storeId))
                .as("앞뒤 공백 제거").isEqualTo("강남점");
    }

    @Test
    @DisplayName("매장코드 · 매장명이 중복되면 409, 형식이 틀리면 400")
    void createStoreValidation() throws Exception {
        insertStore("GN01", "강남점");

        createStore("GN01", "새매장").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_STORE_CODE"));
        createStore("HD01", "강남점").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_STORE_NAME"));
        createStore("gn-1", "").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fieldErrors", hasSize(2)));
    }

    @Test
    @DisplayName("매장 목록: 검색 · 상태 조건과 페이지 정보를 돌려준다")
    void searchStores() throws Exception {
        insertStore("GN01", "강남점");
        insertStore("GN02", "강남역점");
        Long hongdae = insertStore("HD01", "홍대점");
        jdbcTemplate.update("UPDATE stores SET status = 'INACTIVE' WHERE id = ?", hongdae);

        mockMvc.perform(get("/api/v1/stores").param("keyword", "강남").param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].storeCode").value("GN01"))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2));

        mockMvc.perform(get("/api/v1/stores").param("status", "INACTIVE")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].storeName").value("홍대점"));

        mockMvc.perform(get("/api/v1/stores").param("size", "101")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("페이지 크기는 1 이상 100 이하로 입력하세요."));
    }

    @Test
    @DisplayName("매장 상세의 일시는 한국 시간(+09:00)으로 응답하고, 수정 · 사용 중지 · 다시 사용이 된다")
    void detailUpdateAndStatus() throws Exception {
        Long storeId = insertStore("GN01", "강남점");

        mockMvc.perform(get("/api/v1/stores/{id}", storeId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdAt", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00")));

        mockMvc.perform(put("/api/v1/stores/{id}", storeId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("storeName", "강남본점", "address", null, "phone", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(storeId));

        mockMvc.perform(post("/api/v1/stores/{id}/deactivate", storeId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
        mockMvc.perform(post("/api/v1/stores/{id}/activate", storeId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        assertThat(jdbcTemplate.queryForObject("SELECT store_name FROM stores WHERE id = ?", String.class, storeId))
                .isEqualTo("강남본점");
        mockMvc.perform(get("/api/v1/stores/{id}", 999_999).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STORE_NOT_FOUND"));
    }

    @Test
    @DisplayName("매장 관리는 ADMIN 전용: MANAGER · USER는 403")
    void adminOnly() throws Exception {
        Long storeId = insertStore("GN01", "강남점");
        for (Role role : new Role[] {Role.MANAGER, Role.USER}) {
            String token = tokenFor(role, storeId);
            mockMvc.perform(get("/api/v1/stores").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
            mockMvc.perform(get("/api/v1/stores/options").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isForbidden());
        }
    }

    private org.springframework.test.web.servlet.ResultActions createStore(String storeCode, String storeName) throws Exception {
        return mockMvc.perform(post("/api/v1/stores")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("storeCode", storeCode, "storeName", storeName)));
    }

}
