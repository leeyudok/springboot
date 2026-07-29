package com.example.demo.common.error;

import com.example.demo.common.mask.SensitiveMasker;
import com.example.demo.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

/**
 * 전역 예외 처리.
 *
 * <p>모든 예외는 여기서 {@link ApiResponse} 형태로 변환된다. 컨트롤러·서비스에는 try/catch 를 두지 않는다.
 *
 * <p>로깅 정책
 * <ul>
 *   <li>업무 예외({@link BusinessException}) → WARN, 스택트레이스 없이 한 줄</li>
 *   <li>시스템 예외 → ERROR, 스택트레이스 포함</li>
 * </ul>
 * 응답 본문에는 내부 원인을 절대 싣지 않는다 (에러코드 기본 메시지만).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---------------------------------------------------------------
    // 업무 예외
    // ---------------------------------------------------------------

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCodeSpec errorCode = e.getErrorCode();
        log.warn("[BUSINESS] code={} detail={}", errorCode.getCode(), SensitiveMasker.maskAll(e.getDetail()));
        return build(errorCode);
    }

    // ---------------------------------------------------------------
    // 요청 검증
    // ---------------------------------------------------------------

    /** {@code @Valid @RequestBody} 검증 실패 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<FieldErrorDetail>>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        List<FieldErrorDetail> details = new ArrayList<FieldErrorDetail>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            details.add(toDetail(fieldError));
        }
        log.warn("[VALIDATION] fields={}", details.size());
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT, firstReason(details), details));
    }

    /** {@code @ModelAttribute} 바인딩 검증 실패 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<List<FieldErrorDetail>>> handleBind(BindException e) {
        List<FieldErrorDetail> details = new ArrayList<FieldErrorDetail>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            details.add(toDetail(fieldError));
        }
        log.warn("[VALIDATION] fields={}", details.size());
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT, firstReason(details), details));
    }

    /** {@code @Validated} 파라미터 검증 실패 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<FieldErrorDetail>>> handleConstraintViolation(ConstraintViolationException e) {
        List<FieldErrorDetail> details = new ArrayList<FieldErrorDetail>();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            String field = violation.getPropertyPath() == null ? "" : violation.getPropertyPath().toString();
            details.add(new FieldErrorDetail(field, mask(violation.getInvalidValue()), violation.getMessage()));
        }
        log.warn("[VALIDATION] constraints={}", details.size());
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT, firstReason(details), details));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("[VALIDATION] missing parameter: {}", e.getParameterName());
        return ResponseEntity.status(ErrorCode.MISSING_PARAMETER.getHttpStatus())
                .body(ApiResponse.<Void>error(ErrorCode.MISSING_PARAMETER,
                        "필수 파라미터가 누락되었습니다: " + e.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("[VALIDATION] type mismatch: {}", e.getName());
        return ResponseEntity.status(ErrorCode.TYPE_MISMATCH.getHttpStatus())
                .body(ApiResponse.<Void>error(ErrorCode.TYPE_MISMATCH,
                        "파라미터 형식이 올바르지 않습니다: " + e.getName()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("[VALIDATION] malformed body: {}", e.getMostSpecificCause().getClass().getSimpleName());
        return build(ErrorCode.MALFORMED_BODY);
    }

    // ---------------------------------------------------------------
    // 인증 / 인가
    // ---------------------------------------------------------------

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException e) {
        log.warn("[AUTH] {}", e.getClass().getSimpleName());
        return build(ErrorCode.UNAUTHENTICATED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("[AUTH] access denied: {}", e.getMessage());
        return build(ErrorCode.ACCESS_DENIED);
    }

    // ---------------------------------------------------------------
    // 라우팅
    // ---------------------------------------------------------------

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandler(NoHandlerFoundException e) {
        log.warn("[ROUTE] no handler: {} {}", e.getHttpMethod(), e.getRequestURL());
        return build(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("[ROUTE] method not allowed: {}", e.getMethod());
        return build(ErrorCode.METHOD_NOT_ALLOWED);
    }

    // ---------------------------------------------------------------
    // 데이터 계층
    // ---------------------------------------------------------------

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException e) {
        // 제약조건 위반은 업무 규칙 누락일 가능성이 높으므로 ERROR 로 남겨 반드시 추적한다.
        log.error("[DATA] integrity violation", e);
        return build(ErrorCode.DATA_ACCESS_ERROR);
    }

    @ExceptionHandler(QueryTimeoutException.class)
    public ResponseEntity<ApiResponse<Void>> handleQueryTimeout(QueryTimeoutException e) {
        log.error("[DATA] query timeout", e);
        return build(ErrorCode.TIMEOUT);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccess(DataAccessException e) {
        log.error("[DATA] access error", e);
        return build(ErrorCode.DATA_ACCESS_ERROR);
    }

    // ---------------------------------------------------------------
    // 최후 방어선
    // ---------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("[SYSTEM] unhandled exception", e);
        return build(ErrorCode.INTERNAL_ERROR);
    }

    // ---------------------------------------------------------------
    // 내부 유틸
    // ---------------------------------------------------------------

    private ResponseEntity<ApiResponse<Void>> build(ErrorCodeSpec errorCode) {
        HttpStatus status = errorCode.getHttpStatus();
        return ResponseEntity.status(status).body(ApiResponse.<Void>error(errorCode));
    }

    private FieldErrorDetail toDetail(FieldError fieldError) {
        return new FieldErrorDetail(fieldError.getField(), mask(fieldError.getRejectedValue()), fieldError.getDefaultMessage());
    }

    /** 거부된 값은 그대로 회신하면 안 되므로 마스킹을 거친다. */
    private String mask(Object rejectedValue) {
        return rejectedValue == null ? null : SensitiveMasker.maskAll(String.valueOf(rejectedValue));
    }

    private String firstReason(List<FieldErrorDetail> details) {
        return details.isEmpty() ? ErrorCode.INVALID_INPUT.getMessage() : details.get(0).getReason();
    }
}
