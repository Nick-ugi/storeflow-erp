package com.storeflow.auth.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * storeflow.jwt.* 설정. secret은 운영에서 JWT_SECRET 환경변수로 지정한다.
 */
@Validated
@ConfigurationProperties(prefix = "storeflow.jwt")
public record JwtProperties(@NotBlank String secret, @NotNull Duration expiration) {
}
