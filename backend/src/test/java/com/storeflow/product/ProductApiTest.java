package com.storeflow.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
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
import org.springframework.test.web.servlet.ResultActions;

class ProductApiTest extends IntegrationTestSupport {

    private String adminToken;
    private Long categoryId;
    private Long gangnam;
    private Long hongdae;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = adminToken();
        categoryId = insertCategory("상의");
        gangnam = insertStore("GN01", "강남점");
        hongdae = insertStore("HD01", "홍대점");
    }

    @Test
    @DisplayName("상품을 등록하면 모든 매장(사용 중지 매장 포함)의 재고 행이 수량 0으로 생성된다")
    void createProductCreatesStockRows() throws Exception {
        jdbcTemplate.update("UPDATE stores SET status = 'INACTIVE' WHERE id = ?", hongdae);

        String body = createProduct("P-0001", "반팔 티셔츠", categoryId, 12000, 29000)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long productId = ((Number) JsonPath.read(body, "$.data.id")).longValue();

        assertThat(countStocks(gangnam, productId)).isEqualTo(1);
        assertThat(countStocks(hongdae, productId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT sum(quantity) FROM stocks WHERE product_id = ?", Integer.class, productId))
                .isZero();
    }

    @Test
    @DisplayName("상품코드 중복 409, 없는 카테고리 404, 형식 · 가격 범위 오류 400")
    void createValidation() throws Exception {
        insertProduct(categoryId, "P-0001", "반팔 티셔츠", 12000, 29000);

        createProduct("P-0001", "중복", categoryId, 0, 0)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_PRODUCT_CODE"));
        createProduct("P-0002", "카테고리 없음", 999_999L, 0, 0)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"));
        createProduct("p_0003", "형식 오류", categoryId, -1, 100_000_000)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors", hasSize(3)));
    }

    @Test
    @DisplayName("상품 목록의 재고: MANAGER는 요청과 관계없이 소속 매장, ADMIN은 선택 매장 또는 전 매장 합계")
    void stockQuantityByScope() throws Exception {
        Long productId = insertProduct(categoryId, "P-0001", "반팔 티셔츠", 12000, 29000);
        jdbcTemplate.update("INSERT INTO stocks (store_id, product_id, quantity) VALUES (?, ?, 5), (?, ?, 7)",
                gangnam, productId, hongdae, productId);
        String managerToken = tokenFor(Role.MANAGER, gangnam);

        mockMvc.perform(get("/api/v1/products").param("storeId", String.valueOf(hongdae))
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(jsonPath("$.data.content[0].stockQuantity").value(5));
        mockMvc.perform(get("/api/v1/products").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data.content[0].stockQuantity").value(12));
        mockMvc.perform(get("/api/v1/products").param("storeId", String.valueOf(hongdae))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data.content[0].stockQuantity").value(7))
                .andExpect(jsonPath("$.data.content[0].salePrice").value(29000))
                .andExpect(jsonPath("$.data.content[0].categoryName").value("상의"));
    }

    @Test
    @DisplayName("수정 요청에 상품코드가 있어도 바뀌지 않고, 사용 중지 후 상태 조건으로 검색된다")
    void updateAndDeactivate() throws Exception {
        Long productId = insertProduct(categoryId, "P-0001", "반팔 티셔츠", 12000, 29000);

        mockMvc.perform(put("/api/v1/products/{id}", productId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productCode":"CHANGED","productName":"반팔 티셔츠 (화이트)","categoryId":%d,"purchasePrice":12500,"salePrice":31000}
                                """.formatted(categoryId)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/products/{id}", productId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data.productCode").value("P-0001"))
                .andExpect(jsonPath("$.data.productName").value("반팔 티셔츠 (화이트)"))
                .andExpect(jsonPath("$.data.salePrice").value(31000));

        mockMvc.perform(post("/api/v1/products/{id}/deactivate", productId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
        mockMvc.perform(get("/api/v1/products").param("status", "ACTIVE").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data.totalElements").value(0));
        mockMvc.perform(get("/api/v1/products").param("status", "INACTIVE").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("상품 조회는 전 역할, 등록 · 수정 · 사용 중지는 ADMIN 전용")
    void permissions() throws Exception {
        Long productId = insertProduct(categoryId, "P-0001", "반팔 티셔츠", 12000, 29000);
        String userToken = tokenFor(Role.USER, gangnam);

        mockMvc.perform(get("/api/v1/products/{id}", productId).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/products/{id}/deactivate", productId).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/products").param("status", "DELETED").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("입력값 형식이 올바르지 않습니다."));
    }

    private ResultActions createProduct(String code, String name, Long categoryId, long purchasePrice, long salePrice) throws Exception {
        return mockMvc.perform(post("/api/v1/products")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"productCode":"%s","productName":"%s","categoryId":%d,"purchasePrice":%d,"salePrice":%d}
                        """.formatted(code, name, categoryId, purchasePrice, salePrice)));
    }

}
