package com.storeflow.common.code;

/**
 * 역할 (roles.role_name) — 코드 정의서 2.1
 */
public enum Role {

    ADMIN,
    MANAGER,
    USER;

    /** Spring Security 권한 이름 (hasRole('ADMIN') 등에서 사용) */
    public String authority() {
        return "ROLE_" + name();
    }

}
