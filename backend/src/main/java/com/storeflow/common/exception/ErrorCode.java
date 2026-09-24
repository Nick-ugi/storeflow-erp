package com.storeflow.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * API 오류 코드. 코드 이름과 기본 메시지는 docs/api/01-api-common.md 5장을 따른다.
 */
@Getter
public enum ErrorCode {

    // 공통
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값을 확인하세요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다. 다시 로그인하세요."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    API_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 API를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도하세요."),

    // 인증 · 사용자
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_INACTIVE(HttpStatus.FORBIDDEN, "사용이 중지된 계정입니다. 관리자에게 문의하세요."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),
    PASSWORD_REUSED(HttpStatus.BAD_REQUEST, "현재 비밀번호와 다른 비밀번호를 입력하세요."),
    SELF_MODIFICATION_NOT_ALLOWED(HttpStatus.CONFLICT, "본인 계정의 역할과 상태는 변경할 수 없습니다."),
    LAST_ADMIN_REQUIRED(HttpStatus.CONFLICT, "활성 ADMIN이 최소 1명 있어야 합니다."),

    // 데이터 없음
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 매장입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리입니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다."),
    SUPPLIER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 공급처입니다."),
    SALE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 판매입니다."),
    PURCHASE_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 발주입니다."),

    // 중복
    DUPLICATE_STORE_CODE(HttpStatus.CONFLICT, "이미 사용 중인 매장코드입니다."),
    DUPLICATE_STORE_NAME(HttpStatus.CONFLICT, "이미 사용 중인 매장명입니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    DUPLICATE_CATEGORY_NAME(HttpStatus.CONFLICT, "이미 사용 중인 카테고리명입니다."),
    DUPLICATE_PRODUCT_CODE(HttpStatus.CONFLICT, "이미 사용 중인 상품코드입니다."),
    DUPLICATE_BUSINESS_NUMBER(HttpStatus.CONFLICT, "이미 등록된 사업자등록번호입니다."),

    // 업무 상태
    STORE_INACTIVE(HttpStatus.CONFLICT, "사용 중지된 매장입니다."),
    PRODUCT_NOT_AVAILABLE(HttpStatus.CONFLICT, "사용할 수 없는 상품이 포함되어 있습니다."),
    SUPPLIER_NOT_AVAILABLE(HttpStatus.CONFLICT, "발주할 수 없는 공급처입니다."),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다."),
    SALE_ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 판매입니다."),
    INVALID_PURCHASE_ORDER_STATUS(HttpStatus.CONFLICT, "현재 발주 상태에서는 처리할 수 없습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

}
