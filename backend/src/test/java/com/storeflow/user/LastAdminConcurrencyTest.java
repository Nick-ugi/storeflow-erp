package com.storeflow.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.storeflow.common.code.Role;
import com.storeflow.common.exception.BusinessException;
import com.storeflow.common.exception.ErrorCode;
import com.storeflow.common.security.LoginUser;
import com.storeflow.support.ConcurrencyTestSupport;
import com.storeflow.user.dto.UserUpdateRequest;
import com.storeflow.user.service.UserService;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;

class LastAdminConcurrencyTest extends ConcurrencyTestSupport {

    @Autowired
    private UserService userService;

    @RepeatedTest(5)
    @DisplayName("두 ADMIN이 동시에 서로를 MANAGER로 강등해도 한 요청만 성공하고 활성 ADMIN이 1명 남는다")
    void mutualDemotion() throws Exception {
        Long storeId = jdbcTemplate.queryForObject(
                "INSERT INTO stores (store_code, store_name) VALUES ('GN01', '강남점') RETURNING id", Long.class);
        Long adminA = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        Long adminB = jdbcTemplate.queryForObject("""
                INSERT INTO users (username, password, name, role_id)
                VALUES ('admin2', 'x', '관리자2', (SELECT id FROM roles WHERE role_name = 'ADMIN')) RETURNING id
                """, Long.class);
        LoginUser loginA = new LoginUser(adminA, "admin", "관리자", Role.ADMIN, null, null);
        LoginUser loginB = new LoginUser(adminB, "admin2", "관리자2", Role.ADMIN, null, null);

        List<Callable<Object>> tasks = List.of(
                () -> { userService.update(loginA, adminB, new UserUpdateRequest("관리자2", Role.MANAGER, storeId)); return "ok"; },
                () -> { userService.update(loginB, adminA, new UserUpdateRequest("관리자", Role.MANAGER, storeId)); return "ok"; });
        List<Object> results = runConcurrently(tasks);

        assertThat(results).filteredOn("ok"::equals).hasSize(1);
        assertThat(results).filteredOn(BusinessException.class::isInstance)
                .singleElement()
                .extracting("errorCode").isEqualTo(ErrorCode.LAST_ADMIN_REQUIRED);
        Integer activeAdmins = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM users u JOIN roles r ON r.id = u.role_id
                 WHERE r.role_name = 'ADMIN' AND u.status = 'ACTIVE'
                """, Integer.class);
        assertThat(activeAdmins).isEqualTo(1);
    }

}
