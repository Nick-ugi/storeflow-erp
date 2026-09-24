package com.storeflow.common.exception;

import com.storeflow.common.response.ApiErrorResponse;
import com.storeflow.common.response.FieldErrorDetail;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 모든 예외를 공통 오류 응답(ApiErrorResponse)으로 변환한다.
 * Spring Security의 인증 · 인가 오류도 SecurityErrorHandler를 거쳐 여기서 처리된다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String INVALID_FORMAT_MESSAGE = "입력값 형식이 올바르지 않습니다.";

    /** DB 중복 제약 이름 → 오류 코드. 동시에 같은 값을 등록해 서비스의 중복 검사를 통과한 경우에 사용된다. */
    private static final Map<String, ErrorCode> UNIQUE_CONSTRAINT_ERRORS = Map.of(
            "uk_stores_store_code", ErrorCode.DUPLICATE_STORE_CODE,
            "uk_stores_store_name", ErrorCode.DUPLICATE_STORE_NAME,
            "uk_users_username", ErrorCode.DUPLICATE_USERNAME,
            "uk_categories_category_name", ErrorCode.DUPLICATE_CATEGORY_NAME,
            "uk_products_product_code", ErrorCode.DUPLICATE_PRODUCT_CODE,
            "uk_suppliers_business_number", ErrorCode.DUPLICATE_BUSINESS_NUMBER);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException e) {
        return toResponse(e.getErrorCode(), e.getMessage(), e.getFieldErrors());
    }

    /** @Valid 요청 본문 · @ModelAttribute 검증 실패 (MethodArgumentNotValidException 포함) */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBind(BindException e) {
        List<FieldErrorDetail> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toFieldErrorDetail)
                .toList();
        return validationError(fieldErrors);
    }

    /** @RequestParam · @PathVariable 제약 검증 실패 */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodValidation(HandlerMethodValidationException e) {
        List<FieldErrorDetail> fieldErrors = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldErrorDetail(result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        return validationError(fieldErrors);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(Exception e) {
        return toResponse(ErrorCode.VALIDATION_ERROR, INVALID_FORMAT_MESSAGE, List.of());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateKey(DuplicateKeyException e) {
        String detail = String.valueOf(e.getMostSpecificCause().getMessage());
        return UNIQUE_CONSTRAINT_ERRORS.entrySet().stream()
                .filter(entry -> detail.contains("\"" + entry.getKey() + "\""))
                .findFirst()
                .map(entry -> toResponse(entry.getValue(), entry.getValue().getDefaultMessage(), List.of()))
                .orElseGet(() -> handleUnexpected(e));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException e) {
        return toResponse(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getDefaultMessage(), List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException e) {
        return toResponse(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getDefaultMessage(), List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(NoResourceFoundException e) {
        return toResponse(ErrorCode.API_NOT_FOUND, ErrorCode.API_NOT_FOUND.getDefaultMessage(), List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return toResponse(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.getDefaultMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return toResponse(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage(), List.of());
    }

    private static FieldErrorDetail toFieldErrorDetail(FieldError error) {
        String message = error.isBindingFailure() ? INVALID_FORMAT_MESSAGE : error.getDefaultMessage();
        return new FieldErrorDetail(error.getField(), message);
    }

    /** 첫 번째 항목 메시지를 대표 메시지로 사용해 화면이 바로 보여줄 수 있게 한다. */
    private static ResponseEntity<ApiErrorResponse> validationError(List<FieldErrorDetail> fieldErrors) {
        String message = fieldErrors.isEmpty()
                ? ErrorCode.VALIDATION_ERROR.getDefaultMessage()
                : fieldErrors.getFirst().message();
        return toResponse(ErrorCode.VALIDATION_ERROR, message, fieldErrors);
    }

    private static ResponseEntity<ApiErrorResponse> toResponse(ErrorCode errorCode, String message, List<FieldErrorDetail> fieldErrors) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiErrorResponse.of(errorCode, message, fieldErrors));
    }

}
