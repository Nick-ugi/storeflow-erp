package com.storeflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.storeflow.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StoreFlowApplicationTests extends IntegrationTestSupport {

    @Test
    @DisplayName("Flyway 마이그레이션으로 테이블 12개, 역할 3개, 초기 ADMIN 계정이 생성된다")
    void migrationsApplied() {
        Integer tables = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_tables WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'", Integer.class);
        Integer roles = jdbcTemplate.queryForObject("SELECT count(*) FROM roles", Integer.class);
        String adminRole = jdbcTemplate.queryForObject(
                "SELECT r.role_name FROM users u JOIN roles r ON r.id = u.role_id WHERE u.username = 'admin'", String.class);

        assertThat(tables).isEqualTo(12);
        assertThat(roles).isEqualTo(3);
        assertThat(adminRole).isEqualTo("ADMIN");
    }

}
