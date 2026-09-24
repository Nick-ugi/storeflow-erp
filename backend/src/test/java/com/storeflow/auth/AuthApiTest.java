package com.storeflow.auth;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.storeflow.auth.jwt.JwtTokenProvider;
import com.storeflow.common.code.Role;
import com.storeflow.support.IntegrationTestSupport;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

class AuthApiTest extends IntegrationTestSupport {

    private static final String MANAGER_PASSWORD = "manager123";

    @Autowired
    private JwtEncoder jwtEncoder;

    private Long storeId;
    private Long managerId;

    @BeforeEach
    void setUp() {
        storeId = insertStore("GN01", "강남점");
        managerId = insertUser("mgr01", MANAGER_PASSWORD, Role.MANAGER, storeId);
    }

    @Test
    @DisplayName("로그인에 성공하면 Bearer 토큰과 사용자 정보를 받는다")
    void loginSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", "mgr01", "password", MANAGER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken", not(emptyString())))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(28800))
                .andExpect(jsonPath("$.data.user.role").value("MANAGER"))
                .andExpect(jsonPath("$.data.user.storeId").value(storeId))
                .andExpect(jsonPath("$.data.user.storeName").value("강남점"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist());
    }

    @Test
    @DisplayName("비밀번호가 틀려도, 아이디가 없어도 같은 LOGIN_FAILED 오류를 받는다")
    void loginFailedWithSameError() throws Exception {
        for (String[] credentials : new String[][] {{"mgr01", "wrong-password1"}, {"nobody", "whatever123"}}) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("username", credentials[0], "password", credentials[1])))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("LOGIN_FAILED"))
                    .andExpect(jsonPath("$.error.message").value("아이디 또는 비밀번호가 올바르지 않습니다."))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    @Test
    @DisplayName("비활성 사용자는 비밀번호가 맞아도 ACCOUNT_INACTIVE로 로그인할 수 없다")
    void loginInactiveUser() throws Exception {
        jdbcTemplate.update("UPDATE users SET status = 'INACTIVE' WHERE id = ?", managerId);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", "mgr01", "password", MANAGER_PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_INACTIVE"));
    }

    @Test
    @DisplayName("아이디 · 비밀번호를 비우면 VALIDATION_ERROR와 항목별 오류를 받는다")
    void loginValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", "", "password", " ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("아이디와 비밀번호를 입력하세요."))
                .andExpect(jsonPath("$.error.fieldErrors", hasSize(2)));
    }

    @Test
    @DisplayName("토큰으로 내 정보를 조회할 수 있고, ADMIN은 소속 매장이 없다")
    void me() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.storeId", nullValue()));
    }

    @Test
    @DisplayName("토큰이 없거나 위조 · 만료된 토큰이면 401 UNAUTHORIZED를 공통 오류 형식으로 받는다")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer("not.a.valid-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(expiredToken(managerId))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("사용자를 비활성화하면 이미 발급된 토큰도 다음 요청부터 거부된다")
    void deactivatedUserTokenRejectedImmediately() throws Exception {
        String token = login("mgr01", MANAGER_PASSWORD);
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        jdbcTemplate.update("UPDATE users SET status = 'INACTIVE' WHERE id = ?", managerId);

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("역할 · 소속 매장을 바꾸면 이미 발급된 토큰에도 다음 요청부터 반영된다")
    void roleChangeReflectedImmediately() throws Exception {
        String token = login("mgr01", MANAGER_PASSWORD);
        Long otherStoreId = insertStore("HD01", "홍대점");
        jdbcTemplate.update("""
                UPDATE users SET role_id = (SELECT id FROM roles WHERE role_name = 'USER'), store_id = ? WHERE id = ?
                """, otherStoreId, managerId);

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.storeName").value("홍대점"));
    }

    @Test
    @DisplayName("비밀번호 변경: 현재 비밀번호 확인, 같은 비밀번호 금지, 규칙 검사 후 새 비밀번호로 로그인된다")
    void changePassword() throws Exception {
        String token = login("mgr01", MANAGER_PASSWORD);

        changePassword(token, "wrong-current1", "newpass2026")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PASSWORD_MISMATCH"));
        changePassword(token, MANAGER_PASSWORD, MANAGER_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PASSWORD_REUSED"));
        changePassword(token, MANAGER_PASSWORD, "onlyletters")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("newPassword"))
                .andExpect(jsonPath("$.error.message").value("비밀번호는 영문과 숫자를 포함해 8~20자로 입력하세요."));

        changePassword(token, MANAGER_PASSWORD, "newpass2026")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", nullValue()));
        login("mgr01", "newpass2026");
    }

    @Test
    @DisplayName("로그아웃은 인증된 사용자에게 성공 응답을 준다")
    void logout() throws Exception {
        String token = login("mgr01", MANAGER_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private org.springframework.test.web.servlet.ResultActions changePassword(String token, String current, String next) throws Exception {
        return mockMvc.perform(put("/api/v1/auth/password")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("currentPassword", current, "newPassword", next)));
    }

    private String expiredToken(Long userId) {
        Instant issuedAt = Instant.now().minus(9, ChronoUnit.HOURS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtTokenProvider.ISSUER)
                .subject(String.valueOf(userId))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(8, ChronoUnit.HOURS))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

}
