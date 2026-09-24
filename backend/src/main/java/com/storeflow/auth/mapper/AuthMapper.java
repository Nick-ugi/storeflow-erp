package com.storeflow.auth.mapper;

import com.storeflow.auth.domain.AuthUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthMapper {

    AuthUser findByUsername(String username);

    AuthUser findById(Long id);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

}
