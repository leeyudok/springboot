package com.example.demo.common.error;

import lombok.Getter;

/**
 * 업무 규칙 위반 예외.
 *
 * <p>{@link RuntimeException} 을 상속해 선언적 트랜잭션의 기본 롤백 규칙(unchecked → rollback)을 그대로 탄다.
 * 서비스 계층에서는 이 예외만 던지고, 응답 변환은 {@link GlobalExceptionHandler} 가 전담한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    /** 로그·감사용 상세 사유 (대외 응답에는 포함하지 않는다). */
    private final String detail;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }

    /**
     * @param detail 내부 조사용 상세 사유 — 계좌번호·잔액 등 민감정보는 마스킹해서 넘길 것
     */
    public BusinessException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + " (" + detail + ")");
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public BusinessException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode.getMessage() + " (" + detail + ")", cause);
        this.errorCode = errorCode;
        this.detail = detail;
    }
}
