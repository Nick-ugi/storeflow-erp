package com.storeflow.user.dto;

import com.storeflow.common.code.ActiveStatus;
import com.storeflow.common.code.Role;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 사용자 목록 항목 (API-USER-001). 비밀번호는 어떤 응답에도 포함하지 않는다.
 */
@Getter
@Setter
public class UserResponse {

    private Long id;
    private String username;
    private String name;
    private Role role;
    private Long storeId;
    private String storeName;
    private ActiveStatus status;
    private OffsetDateTime createdAt;

}
