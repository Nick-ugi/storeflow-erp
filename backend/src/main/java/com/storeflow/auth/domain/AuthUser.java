package com.storeflow.auth.domain;

import com.storeflow.common.code.ActiveStatus;
import com.storeflow.common.code.Role;
import com.storeflow.common.security.LoginUser;
import lombok.Getter;
import lombok.Setter;

/**
 * 인증에 필요한 사용자 정보 (users + roles + stores 조회 결과). 비밀번호 해시를 포함하므로 응답에 직접 쓰지 않는다.
 */
@Getter
@Setter
public class AuthUser {

    private Long id;
    private String username;
    private String password;
    private String name;
    private Role role;
    private Long storeId;
    private String storeName;
    private ActiveStatus status;

    public LoginUser toLoginUser() {
        return new LoginUser(id, username, name, role, storeId, storeName);
    }

}
