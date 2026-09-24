package com.storeflow.user.dto;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 사용자 상세 (API-USER-002) = 목록 항목 + 수정 일시
 */
@Getter
@Setter
public class UserDetailResponse extends UserResponse {

    private OffsetDateTime updatedAt;

}
