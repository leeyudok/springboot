package com.example.demo.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 전사 공통 에러코드.
 *
 * <p>코드 체계 — 4자리 숫자, 앞 2자리가 분류:
 * <ul>
 *   <li>{@code 00xx} : 정상 / 공통</li>
 *   <li>{@code 10xx} : 요청 검증</li>
 *   <li>{@code 20xx} : 인증 / 인가</li>
 *   <li>{@code 30xx} : 업무 규칙 (회원)</li>
 *   <li>{@code 40xx} : 업무 규칙 (계좌 / 거래)</li>
 *   <li>{@code 90xx} : 시스템 / 연계</li>
 * </ul>
 *
 * <p>메시지는 <b>대외 노출용</b>이다. 내부 원인(스택트레이스, SQL, 키 값)은 절대 넣지 말고
 * 로그에만 남긴다.
 */
@Getter
public enum ErrorCode {

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

    // --- 회원 ---------------------------------------------------
    MEMBER_NOT_FOUND("3000", HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."),
    DUPLICATE_LOGIN_ID("3001", HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),

    // --- 계좌 / 거래 --------------------------------------------
    ACCOUNT_NOT_FOUND("4000", HttpStatus.NOT_FOUND, "계좌 정보를 찾을 수 없습니다."),
    ACCOUNT_NOT_ACTIVE("4001", HttpStatus.BAD_REQUEST, "거래할 수 없는 상태의 계좌입니다."),
    INSUFFICIENT_BALANCE("4002", HttpStatus.BAD_REQUEST, "잔액이 부족합니다."),
    SAME_ACCOUNT_TRANSFER("4003", HttpStatus.BAD_REQUEST, "출금계좌와 입금계좌가 동일합니다."),
    TRANSFER_LIMIT_EXCEEDED("4004", HttpStatus.BAD_REQUEST, "1회 이체 한도를 초과했습니다."),
    CURRENCY_MISMATCH("4005", HttpStatus.BAD_REQUEST, "통화가 서로 다른 계좌 간에는 이체할 수 없습니다."),
    NOT_ACCOUNT_OWNER("4006", HttpStatus.FORBIDDEN, "본인 명의 계좌가 아닙니다."),

    // --- 시스템 / 연계 ------------------------------------------
    DATA_ACCESS_ERROR("9001", HttpStatus.INTERNAL_SERVER_ERROR, "데이터 처리 중 오류가 발생했습니다."),
    CONCURRENT_UPDATE("9002", HttpStatus.CONFLICT, "다른 거래가 처리 중입니다. 잠시 후 다시 시도해 주세요."),
    EXTERNAL_API_ERROR("9003", HttpStatus.BAD_GATEWAY, "외부 기관 연계 중 오류가 발생했습니다."),
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
