package com.storeflow.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.storeflow.TestcontainersConfiguration;
import com.storeflow.common.code.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * API 통합 테스트 공통 설정. 테스트마다 트랜잭션을 롤백하므로 테스트 간 데이터가 섞이지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
public abstract class IntegrationTestSupport {

    protected static final String ADMIN_USERNAME = "admin";
    protected static final String ADMIN_PASSWORD = "admin1234";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("username", username, "password", password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.accessToken");
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected Long insertStore(String storeCode, String storeName) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO stores (store_code, store_name) VALUES (?, ?) RETURNING id", Long.class, storeCode, storeName);
    }

    protected Long insertUser(String username, String rawPassword, Role role, Long storeId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO users (username, password, name, role_id, store_id)
                VALUES (?, ?, ?, (SELECT id FROM roles WHERE role_name = ?), ?)
                RETURNING id
                """, Long.class, username, passwordEncoder.encode(rawPassword), username + " 이름", role.name(), storeId);
    }

    /** 문자열 키 · 값 쌍으로 간단한 JSON 객체를 만든다. */
    protected static String json(String... keyValues) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(keyValues[i]).append("\":");
            sb.append(keyValues[i + 1] == null ? "null" : '"' + keyValues[i + 1] + '"');
        }
        return sb.append('}').toString();
    }

}
