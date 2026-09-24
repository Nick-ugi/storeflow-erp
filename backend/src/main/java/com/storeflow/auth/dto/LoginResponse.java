package com.storeflow.auth.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserInfoResponse user) {

    public static LoginResponse bearer(String accessToken, long expiresIn, UserInfoResponse user) {
        return new LoginResponse(accessToken, "Bearer", expiresIn, user);
    }

}
