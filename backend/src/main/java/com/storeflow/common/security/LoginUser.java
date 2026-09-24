package com.storeflow.common.security;

import com.storeflow.common.code.Role;
import com.storeflow.common.exception.BusinessException;
import com.storeflow.common.exception.ErrorCode;
import java.util.Objects;

/**
 * 요청마다 DB에서 다시 읽은 로그인 사용자. 컨트롤러에서 {@code @AuthenticationPrincipal}로 받는다.
 * 매장 데이터 범위 규칙(API 공통 규칙 3장, BR-002)을 한곳에서 처리한다.
 */
public record LoginUser(Long id, String username, String name, Role role, Long storeId, String storeName) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /** 목록 · 조회용 매장 범위. ADMIN은 요청한 매장(null이면 전 매장), MANAGER · USER는 요청과 관계없이 소속 매장 */
    public Long scopeStoreId(Long requestedStoreId) {
        return isAdmin() ? requestedStoreId : storeId;
    }

    /** 등록 · 단일 매장 처리용. ADMIN은 매장 지정 필수, MANAGER · USER는 소속 매장 */
    public Long requireStoreId(Long requestedStoreId, String messageWhenMissing) {
        if (!isAdmin()) {
            return storeId;
        }
        if (requestedStoreId == null) {
            throw BusinessException.invalidField("storeId", messageWhenMissing);
        }
        return requestedStoreId;
    }

    /** 다른 매장의 판매 · 발주 상세에 MANAGER · USER가 접근하면 거부한다. */
    public void checkStoreAccess(Long targetStoreId) {
        if (!isAdmin() && !Objects.equals(storeId, targetStoreId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

}
