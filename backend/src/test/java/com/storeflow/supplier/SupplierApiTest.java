package com.storeflow.supplier;

import static org.assertj.core.api.Assertions.assertThat;
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

class SupplierApiTest extends IntegrationTestSupport {

    private String adminToken;
    private Long storeId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = adminToken();
        storeId = insertStore("GN01", "강남점");
    }

    @Test
    @DisplayName("사업자등록번호는 하이픈을 빼고 숫자만 저장하며, 형식이 달라도 같은 번호면 중복이다")
    void businessNumberNormalizedAndUnique() throws Exception {
        String body = createSupplier("한빛상사", "123-45-67890").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long supplierId = ((Number) JsonPath.read(body, "$.data.id")).longValue();
        assertThat(jdbcTemplate.queryForObject("SELECT business_number FROM suppliers WHERE id = ?", String.class, supplierId))
                .isEqualTo("1234567890");

        createSupplier("다른상사", "1234567890")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_BUSINESS_NUMBER"));
        createSupplier("형식오류", "123-45-678")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("사업자등록번호는 숫자 10자리로 입력하세요."));
    }

    @Test
    @DisplayName("권한: 목록은 전 역할, 선택 목록은 ADMIN · MANAGER, 상세 · 등록은 ADMIN")
    void permissions() throws Exception {
        Long supplierId = insertSupplier("한빛상사", "1234567890");
        String managerToken = tokenFor(Role.MANAGER, storeId);
        String userToken = tokenFor(Role.USER, storeId);

        mockMvc.perform(get("/api/v1/suppliers").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].supplierName").value("한빛상사"));
        mockMvc.perform(get("/api/v1/suppliers/options").header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
        mockMvc.perform(get("/api/v1/suppliers/options").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/suppliers/{id}", supplierId).header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("수정 · 사용 중지 후 선택 목록에 사용 중지 상태로 나온다")
    void updateAndDeactivate() throws Exception {
        Long supplierId = insertSupplier("한빛상사", "1234567890");

        mockMvc.perform(put("/api/v1/suppliers/{id}", supplierId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("supplierName", "한빛상사(주)", "businessNumber", "1234567890", "contactName", "이담당", "phone", "02-123-4567")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/suppliers/{id}/deactivate", supplierId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mockMvc.perform(get("/api/v1/suppliers/options").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data[0].supplierName").value("한빛상사(주)"))
                .andExpect(jsonPath("$.data[0].status").value("INACTIVE"));
    }

    private Long insertSupplier(String name, String businessNumber) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO suppliers (supplier_name, business_number) VALUES (?, ?) RETURNING id", Long.class, name, businessNumber);
    }

    private ResultActions createSupplier(String name, String businessNumber) throws Exception {
        return mockMvc.perform(post("/api/v1/suppliers")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("supplierName", name, "businessNumber", businessNumber)));
    }

}
