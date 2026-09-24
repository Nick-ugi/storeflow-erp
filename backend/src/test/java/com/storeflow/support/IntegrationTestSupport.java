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
 * 서비스도 테스트 트랜잭션에 참여하므로, 실제 롤백 · 동시성을 검증하려면 RealTransactionTestSupport를 쓴다.
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

    protected TestFixtures fixtures() {
        return new TestFixtures(jdbcTemplate, passwordEncoder);
    }

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

    protected String adminToken() throws Exception {
        return login(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    /** 역할 · 소속 매장을 가진 테스트 사용자를 만들고 토큰을 발급받는다. */
    protected String tokenFor(Role role, Long storeId) throws Exception {
        String username = role.name().toLowerCase() + System.nanoTime() % 100_000_000;
        insertUser(username, "test1234", role, storeId);
        return login(username, "test1234");
    }

    protected Long insertStore(String storeCode, String storeName) {
        return fixtures().insertStore(storeCode, storeName);
    }

    protected Long insertUser(String username, String rawPassword, Role role, Long storeId) {
        return fixtures().insertUser(username, rawPassword, role, storeId);
    }

    protected Long insertCategory(String categoryName) {
        return fixtures().insertCategory(categoryName);
    }

    protected Long insertProduct(Long categoryId, String productCode, String productName, long purchasePrice, long salePrice) {
        return fixtures().insertProduct(categoryId, productCode, productName, purchasePrice, salePrice);
    }

    protected Long countStocks(Long storeId, Long productId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM stocks WHERE store_id = ? AND product_id = ?", Long.class, storeId, productId);
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
