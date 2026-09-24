package com.storeflow.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.storeflow.common.code.Role;
import com.storeflow.common.exception.BusinessException;
import com.storeflow.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 매장 데이터 범위 규칙 (API 공통 규칙 3장, BR-002)
 */
class LoginUserTest {

    private final LoginUser admin = new LoginUser(1L, "admin", "관리자", Role.ADMIN, null, null);
    private final LoginUser manager = new LoginUser(2L, "mgr01", "김매니저", Role.MANAGER, 10L, "강남점");
    private final LoginUser user = new LoginUser(3L, "staff01", "박직원", Role.USER, 10L, "강남점");

    @Test
    @DisplayName("목록 조회: ADMIN은 요청한 매장(없으면 전체), MANAGER · USER는 요청과 관계없이 소속 매장")
    void scopeStoreId() {
        assertThat(admin.scopeStoreId(20L)).isEqualTo(20L);
        assertThat(admin.scopeStoreId(null)).isNull();
        assertThat(manager.scopeStoreId(20L)).isEqualTo(10L);
        assertThat(user.scopeStoreId(null)).isEqualTo(10L);
    }

    @Test
    @DisplayName("등록: ADMIN은 매장 지정 필수, MANAGER · USER는 소속 매장")
    void requireStoreId() {
        assertThat(admin.requireStoreId(20L, "매장을 선택하세요.")).isEqualTo(20L);
        assertThat(manager.requireStoreId(20L, "매장을 선택하세요.")).isEqualTo(10L);
        assertThatThrownBy(() -> admin.requireStoreId(null, "판매할 매장을 선택하세요."))
                .isInstanceOf(BusinessException.class)
                .hasMessage("판매할 매장을 선택하세요.")
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("상세 접근: MANAGER · USER가 다른 매장 데이터에 접근하면 FORBIDDEN")
    void checkStoreAccess() {
        assertThatCode(() -> admin.checkStoreAccess(99L)).doesNotThrowAnyException();
        assertThatCode(() -> manager.checkStoreAccess(10L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> user.checkStoreAccess(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
    }

}
