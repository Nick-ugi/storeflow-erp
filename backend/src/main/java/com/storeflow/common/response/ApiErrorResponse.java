package com.storeflow.common.response;

import com.storeflow.common.exception.ErrorCode;
import java.util.List;

/**
 * 오류 응답: {@code { "success": false, "error": { "code", "message", "fieldErrors" } }}
 */
public record ApiErrorResponse(boolean success, ErrorDetail error) {

    public record ErrorDetail(String code, String message, List<FieldErrorDetail> fieldErrors) {
    }

    public static ApiErrorResponse of(ErrorCode errorCode, String message, List<FieldErrorDetail> fieldErrors) {
        return new ApiErrorResponse(false, new ErrorDetail(errorCode.name(), message, fieldErrors));
    }

}
