package com.storeflow.user.service;

import com.storeflow.common.code.ActiveStatus;
import com.storeflow.common.code.Role;
import com.storeflow.common.exception.BusinessException;
import com.storeflow.common.exception.ErrorCode;
import com.storeflow.common.response.PageResponse;
import com.storeflow.common.response.StatusResponse;
import com.storeflow.common.security.LoginUser;
import com.storeflow.store.service.StoreService;
import com.storeflow.user.dto.PasswordResetRequest;
import com.storeflow.user.dto.UserCreateRequest;
import com.storeflow.user.dto.UserDetailResponse;
import com.storeflow.user.dto.UserResponse;
import com.storeflow.user.dto.UserSearchRequest;
import com.storeflow.user.dto.UserUpdateRequest;
import com.storeflow.user.mapper.UserMapper;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 관리 (인증 · 시스템 관리 명세 4장)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final String INACTIVE_STORE_MESSAGE = "사용 중지된 매장은 소속 매장으로 선택할 수 없습니다.";

    private final UserMapper userMapper;
    private final StoreService storeService;
    private final PasswordEncoder passwordEncoder;

    public PageResponse<UserResponse> search(UserSearchRequest cond) {
        List<UserResponse> content = userMapper.findUsers(cond, cond.offset(), cond.pageSize());
        return PageResponse.of(content, cond, userMapper.countUsers(cond));
    }

    public UserDetailResponse get(Long id) {
        UserDetailResponse user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    @Transactional
    public Long create(UserCreateRequest request) {
        validateStoreForRole(request.role(), request.storeId(), null);
        if (userMapper.existsByUsername(request.username())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }
        return userMapper.insert(request, passwordEncoder.encode(request.password()));
    }

    @Transactional
    public void update(LoginUser loginUser, Long id, UserUpdateRequest request) {
        UserDetailResponse user = get(id);
        if (request.role() != user.getRole()) {
            ensureNotSelf(loginUser, id);
            if (isActiveAdmin(user)) {
                ensureNotLastActiveAdmin(id);
            }
        }
        validateStoreForRole(request.role(), request.storeId(), user.getStoreId());
        userMapper.update(id, request);
    }

    @Transactional
    public StatusResponse<ActiveStatus> deactivate(LoginUser loginUser, Long id) {
        UserDetailResponse user = get(id);
        ensureNotSelf(loginUser, id);
        if (isActiveAdmin(user)) {
            ensureNotLastActiveAdmin(id);
        }
        userMapper.updateStatus(id, ActiveStatus.INACTIVE);
        return new StatusResponse<>(id, ActiveStatus.INACTIVE);
    }

    @Transactional
    public StatusResponse<ActiveStatus> activate(Long id) {
        get(id);
        userMapper.updateStatus(id, ActiveStatus.ACTIVE);
        return new StatusResponse<>(id, ActiveStatus.ACTIVE);
    }

    @Transactional
    public void resetPassword(Long id, PasswordResetRequest request) {
        get(id);
        userMapper.updatePassword(id, passwordEncoder.encode(request.newPassword()));
    }

    /** ADMIN은 소속 매장이 없고, MANAGER · USER는 사용 중인 매장 1곳에 소속된다. (BR-011) */
    private void validateStoreForRole(Role role, Long storeId, Long currentStoreId) {
        if (role == Role.ADMIN) {
            if (storeId != null) {
                throw BusinessException.invalidField("storeId", "ADMIN은 소속 매장을 선택할 수 없습니다.");
            }
            return;
        }
        if (storeId == null) {
            throw BusinessException.invalidField("storeId", "MANAGER와 USER는 소속 매장을 선택해야 합니다.");
        }
        // 이미 소속된 매장이 나중에 사용 중지된 경우, 매장을 바꾸지 않는 수정은 허용한다.
        if (!Objects.equals(storeId, currentStoreId)) {
            storeService.requireActive(storeId, INACTIVE_STORE_MESSAGE);
        }
    }

    /** ADMIN은 자기 역할 · 상태를 바꿀 수 없다. */
    private static void ensureNotSelf(LoginUser loginUser, Long targetId) {
        if (loginUser.id().equals(targetId)) {
            throw new BusinessException(ErrorCode.SELF_MODIFICATION_NOT_ALLOWED);
        }
    }

    private static boolean isActiveAdmin(UserResponse user) {
        return user.getRole() == Role.ADMIN && user.getStatus() == ActiveStatus.ACTIVE;
    }

    /**
     * 활성 ADMIN 행을 잠근 뒤 센다. 두 ADMIN이 동시에 서로를 강등해도 한 요청은 잠금을 기다린 뒤
     * 줄어든 인원을 보게 되므로 활성 ADMIN이 0명이 되지 않는다.
     */
    private void ensureNotLastActiveAdmin(Long targetId) {
        List<Long> activeAdminIds = userMapper.lockActiveAdminIds();
        if (activeAdminIds.size() <= 1 && activeAdminIds.contains(targetId)) {
            throw new BusinessException(ErrorCode.LAST_ADMIN_REQUIRED);
        }
    }

}
