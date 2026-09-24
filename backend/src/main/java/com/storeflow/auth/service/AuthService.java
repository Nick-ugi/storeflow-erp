package com.storeflow.auth.service;

import com.storeflow.auth.domain.AuthUser;
import com.storeflow.auth.dto.LoginRequest;
import com.storeflow.auth.dto.LoginResponse;
import com.storeflow.auth.dto.PasswordChangeRequest;
import com.storeflow.auth.dto.UserInfoResponse;
import com.storeflow.auth.jwt.JwtTokenProvider;
import com.storeflow.auth.mapper.AuthMapper;
import com.storeflow.common.code.ActiveStatus;
import com.storeflow.common.exception.BusinessException;
import com.storeflow.common.exception.ErrorCode;
import com.storeflow.common.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 아이디가 없거나 비밀번호가 틀리면 같은 오류로 응답하고, 비활성 여부는 비밀번호가 맞을 때만 알려준다. (인증 명세 1.2)
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        AuthUser user = authMapper.findByUsername(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (user.getStatus() != ActiveStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }
        String accessToken = jwtTokenProvider.createToken(user.getId());
        return LoginResponse.bearer(accessToken, jwtTokenProvider.expiresInSeconds(), UserInfoResponse.from(user.toLoginUser()));
    }

    @Transactional
    public void changePassword(LoginUser loginUser, PasswordChangeRequest request) {
        AuthUser user = authMapper.findById(loginUser.id());
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_REUSED);
        }
        authMapper.updatePassword(user.getId(), passwordEncoder.encode(request.newPassword()));
    }

}
