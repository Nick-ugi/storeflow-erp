package com.storeflow.auth.jwt;

import com.storeflow.auth.domain.AuthUser;
import com.storeflow.auth.mapper.AuthMapper;
import com.storeflow.common.code.ActiveStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * 검증된 JWT의 사용자 ID로 DB에서 사용자를 다시 읽어 인증 정보를 만든다.
 * 비활성화 · 역할 변경 · 소속 매장 변경이 토큰 만료를 기다리지 않고 다음 요청부터 바로 반영된다.
 */
@Component
@RequiredArgsConstructor
public class LoginUserAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AuthMapper authMapper;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        AuthUser user = authMapper.findById(parseUserId(jwt.getSubject()));
        if (user == null || user.getStatus() != ActiveStatus.ACTIVE) {
            throw new DisabledException("존재하지 않거나 비활성화된 사용자");
        }
        return new UsernamePasswordAuthenticationToken(
                user.toLoginUser(), jwt, List.of(new SimpleGrantedAuthority(user.getRole().authority())));
    }

    private static Long parseUserId(String subject) {
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException e) {
            throw new BadCredentialsException("잘못된 토큰 subject");
        }
    }

}
