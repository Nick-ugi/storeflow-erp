package com.storeflow.auth.dto;

import com.storeflow.common.code.Role;
import com.storeflow.common.security.LoginUser;

public record UserInfoResponse(Long id, String username, String name, Role role, Long storeId, String storeName) {

    public static UserInfoResponse from(LoginUser user) {
        return new UserInfoResponse(user.id(), user.username(), user.name(), user.role(), user.storeId(), user.storeName());
    }

}
