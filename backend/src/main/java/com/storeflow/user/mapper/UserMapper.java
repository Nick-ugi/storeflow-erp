package com.storeflow.user.mapper;

import com.storeflow.common.code.ActiveStatus;
import com.storeflow.user.dto.UserCreateRequest;
import com.storeflow.user.dto.UserDetailResponse;
import com.storeflow.user.dto.UserResponse;
import com.storeflow.user.dto.UserSearchRequest;
import com.storeflow.user.dto.UserUpdateRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    List<UserResponse> findUsers(@Param("cond") UserSearchRequest cond, @Param("offset") int offset, @Param("limit") int limit);

    long countUsers(@Param("cond") UserSearchRequest cond);

    UserDetailResponse findById(Long id);

    boolean existsByUsername(String username);

    Long insert(@Param("request") UserCreateRequest request, @Param("encodedPassword") String encodedPassword);

    int update(@Param("id") Long id, @Param("request") UserUpdateRequest request);

    int updateStatus(@Param("id") Long id, @Param("status") ActiveStatus status);

    int updatePassword(@Param("id") Long id, @Param("encodedPassword") String encodedPassword);

    /** 활성 ADMIN 행을 잠그고 ID를 돌려준다. 마지막 ADMIN 보호 검사가 동시에 실행되어도 순서대로 처리된다. */
    List<Long> lockActiveAdminIds();

}
