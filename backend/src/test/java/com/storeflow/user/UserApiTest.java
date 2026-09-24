package com.storeflow.user;

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

class UserApiTest extends IntegrationTestSupport {

    private String adminToken;
    private Long adminId;
    private Long storeId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = adminToken();
        adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        storeId = insertStore("GN01", "강남점");
    }

    @Test
    @DisplayName("사용자를 등록하면 목록 · 상세에 나오고 비밀번호는 어떤 응답에도 없다")
    void createAndRead() throws Exception {
        String body = createUser("staff01", "welcome123", "박직원", "USER", storeId)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long userId = ((Number) JsonPath.read(body, "$.data.id")).longValue();

        mockMvc.perform(get("/api/v1/users").param("role", "USER").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].username").value("staff01"))
                .andExpect(jsonPath("$.data.content[0].storeName").value("강남점"))
                .andExpect(jsonPath("$.data.content[0].password").doesNotExist());
        mockMvc.perform(get("/api/v1/users/{id}", userId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.updatedAt").exists())
                .andExpect(jsonPath("$.data.password").doesNotExist());

        login("staff01", "welcome123");
    }

    @Test
    @DisplayName("역할별 소속 매장 규칙: MANAGER · USER는 필수, ADMIN은 지정 불가, 사용 중지 매장은 선택 불가")
    void storeRuleByRole() throws Exception {
        createUser("mgr01", "welcome123", "김매니저", "MANAGER", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("storeId"))
                .andExpect(jsonPath("$.error.message").value("MANAGER와 USER는 소속 매장을 선택해야 합니다."));
        createUser("admin2", "welcome123", "관리자2", "ADMIN", storeId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("ADMIN은 소속 매장을 선택할 수 없습니다."));

        Long closedStore = insertStore("HD01", "홍대점");
        jdbcTemplate.update("UPDATE stores SET status = 'INACTIVE' WHERE id = ?", closedStore);
        createUser("mgr02", "welcome123", "이매니저", "MANAGER", closedStore)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("STORE_INACTIVE"));
    }

    @Test
    @DisplayName("아이디 중복은 409, 아이디 · 비밀번호 형식 오류는 400")
    void createValidation() throws Exception {
        createUser("admin", "welcome123", "중복", "USER", storeId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_USERNAME"));
        createUser("Bad-ID", "short", "형식", "USER", storeId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors", hasSize(2)));
    }

    @Test
    @DisplayName("ADMIN은 자기 계정을 비활성화하거나 자기 역할을 바꿀 수 없다")
    void selfModificationBlocked() throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/deactivate", adminId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SELF_MODIFICATION_NOT_ALLOWED"));

        mockMvc.perform(put("/api/v1/users/{id}", adminId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"관리자\",\"role\":\"MANAGER\",\"storeId\":" + storeId + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SELF_MODIFICATION_NOT_ALLOWED"));

        mockMvc.perform(put("/api/v1/users/{id}", adminId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"총괄 관리자\",\"role\":\"ADMIN\",\"storeId\":null}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("다른 사용자 비활성화 · 다시 활성화 · 비밀번호 초기화")
    void statusAndPasswordReset() throws Exception {
        Long userId = insertUser("staff01", "welcome123", Role.USER, storeId);

        mockMvc.perform(post("/api/v1/users/{id}/deactivate", userId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
        mockMvc.perform(post("/api/v1/users/{id}/activate", userId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/users/{id}/reset-password", userId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("newPassword", "reset2026a")))
                .andExpect(status().isOk());
        login("staff01", "reset2026a");
    }

    @Test
    @DisplayName("소속 매장이 나중에 사용 중지돼도 매장을 바꾸지 않는 수정은 되고, 사용 중지 매장으로 옮기는 것은 막는다")
    void updateWithInactiveStore() throws Exception {
        Long userId = insertUser("staff01", "welcome123", Role.USER, storeId);
        jdbcTemplate.update("UPDATE stores SET status = 'INACTIVE' WHERE id = ?", storeId);

        updateUser(userId, "박직원(수정)", "USER", storeId).andExpect(status().isOk());

        Long otherClosed = insertStore("HD01", "홍대점");
        jdbcTemplate.update("UPDATE stores SET status = 'INACTIVE' WHERE id = ?", otherClosed);
        updateUser(userId, "박직원", "USER", otherClosed)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("STORE_INACTIVE"));
    }

    @Test
    @DisplayName("사용자 관리는 ADMIN 전용")
    void adminOnly() throws Exception {
        String managerToken = tokenFor(Role.MANAGER, storeId);
        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isForbidden());
    }

    private ResultActions createUser(String username, String password, String name, String role, Long storeId) throws Exception {
        return mockMvc.perform(post("/api/v1/users")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s","name":"%s","role":"%s","storeId":%s}
                        """.formatted(username, password, name, role, storeId)));
    }

    private ResultActions updateUser(Long id, String name, String role, Long storeId) throws Exception {
        return mockMvc.perform(put("/api/v1/users/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\",\"role\":\"%s\",\"storeId\":%s}".formatted(name, role, storeId)));
    }

}
