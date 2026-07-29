package com.example.demo.sample.ledger;

import com.example.demo.common.error.ErrorCodeSpec;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 원장 샘플 에러코드 ({@code 40xx} 대역).
 *
 * <p>{@code sample} 패키지와 함께 통째로 삭제하는 대상이다.
 */
@Getter
public enum LedgerErrorCode implements ErrorCodeSpec {

    ACCOUNT_NOT_FOUND("4000", HttpStatus.NOT_FOUND, "계정 정보를 찾을 수 없습니다."),
    ACCOUNT_NOT_ACTIVE("4001", HttpStatus.BAD_REQUEST, "거래할 수 없는 상태입니다."),
    INSUFFICIENT_BALANCE("4002", HttpStatus.BAD_REQUEST, "잔액이 부족합니다."),
    SAME_ACCOUNT_TRANSFER("4003", HttpStatus.BAD_REQUEST, "출금 계정과 입금 계정이 동일합니다."),
    TRANSFER_LIMIT_EXCEEDED("4004", HttpStatus.BAD_REQUEST, "1회 처리 한도를 초과했습니다."),
    CURRENCY_MISMATCH("4005", HttpStatus.BAD_REQUEST, "단위가 서로 다른 계정 간에는 이동할 수 없습니다."),
    NOT_ACCOUNT_OWNER("4006", HttpStatus.FORBIDDEN, "본인 소유 계정이 아닙니다.");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    LedgerErrorCode(String code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
