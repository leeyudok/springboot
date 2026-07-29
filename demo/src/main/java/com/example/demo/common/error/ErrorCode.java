package com.example.demo.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 공통 에러코드.
 *
 * <p>어느 프로젝트에 이식하더라도 그대로 쓰는 대역만 담는다 — 공통({@code 00xx}), 요청 검증({@code 10xx}),
 * 인증/인가({@code 20xx}), 시스템/연계({@code 90xx}).
 *
 * <p>업무 도메인 코드는 여기에 추가하지 말고 {@link ErrorCodeSpec} 을 구현한 도메인별 enum 에 둔다
 * (예: {@code MemberErrorCode}, {@code LedgerErrorCode}). 공통 계층이 도메인 지식을 갖지 않게 하기 위함이다.
 */
@Getter
public enum ErrorCode implements ErrorCodeSpec {

    // --- 공통 ---------------------------------------------------
    INTERNAL_ERROR("9000", HttpStatus.INTERNAL_SERVER_ERROR, "시스템 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),
    NOT_FOUND("0004", HttpStatus.NOT_FOUND, "요청하신 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED("0005", HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 요청 방식입니다."),

    // --- 요청 검증 ----------------------------------------------
    INVALID_INPUT("1000", HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    MISSING_PARAMETER("1001", HttpStatus.BAD_REQUEST, "필수 파라미터가 누락되었습니다."),
    TYPE_MISMATCH("1002", HttpStatus.BAD_REQUEST, "파라미터 형식이 올바르지 않습니다."),
    MALFORMED_BODY("1003", HttpStatus.BAD_REQUEST, "요청 본문을 해석할 수 없습니다."),

    // --- 인증 / 인가 --------------------------------------------
    UNAUTHENTICATED("2000", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_CREDENTIALS("2001", HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    TOKEN_EXPIRED("2002", HttpStatus.UNAUTHORIZED, "인증 토큰이 만료되었습니다. 다시 로그인해 주세요."),
    TOKEN_INVALID("2003", HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다."),
    ACCESS_DENIED("2004", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    ACCOUNT_LOCKED("2005", HttpStatus.UNAUTHORIZED, "로그인 실패 횟수 초과로 계정이 잠겼습니다. 관리자에게 문의해 주세요."),
    ACCOUNT_INACTIVE("2006", HttpStatus.UNAUTHORIZED, "사용할 수 없는 계정입니다."),
    TOO_MANY_REQUESTS("2007", HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),

    // --- 시스템 / 연계 ------------------------------------------
    DATA_ACCESS_ERROR("9001", HttpStatus.INTERNAL_SERVER_ERROR, "데이터 처리 중 오류가 발생했습니다."),
    CONCURRENT_UPDATE("9002", HttpStatus.CONFLICT, "다른 처리가 진행 중입니다. 잠시 후 다시 시도해 주세요."),
    EXTERNAL_API_ERROR("9003", HttpStatus.BAD_GATEWAY, "외부 연계 중 오류가 발생했습니다."),
    TIMEOUT("9004", HttpStatus.GATEWAY_TIMEOUT, "처리 시간이 초과되었습니다.");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(String code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
