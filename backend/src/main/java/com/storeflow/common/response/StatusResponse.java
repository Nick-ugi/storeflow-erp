package com.storeflow.common.response;

/**
 * 상태 변경 응답: {@code { "id": 1, "status": "INACTIVE" }}
 */
public record StatusResponse<S extends Enum<S>>(Long id, S status) {
}
