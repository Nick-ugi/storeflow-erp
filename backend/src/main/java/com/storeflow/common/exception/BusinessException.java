package com.storeflow.common.exception;

import com.storeflow.common.response.FieldErrorDetail;
import java.util.List;
import lombok.Getter;

/**
 * 업무 규칙 위반을 나타내는 예외. GlobalExceptionHandler가 오류 응답으로 변환한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<FieldErrorDetail> fieldErrors;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    private BusinessException(ErrorCode errorCode, String message, List<FieldErrorDetail> fieldErrors) {
        super(message);
        this.errorCode = errorCode;
        this.fieldErrors = fieldErrors;
    }

    /** 서비스에서 발견한 입력값 오류 (예: ADMIN이 매장을 선택하지 않음) */
    public static BusinessException invalidField(String field, String message) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, message, List.of(new FieldErrorDetail(field, message)));
    }

}
