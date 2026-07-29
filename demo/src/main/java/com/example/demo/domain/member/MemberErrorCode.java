package com.example.demo.domain.member;

import com.example.demo.common.error.ErrorCodeSpec;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 회원 도메인 에러코드 ({@code 30xx} 대역).
 *
 * <p>도메인별 에러코드 enum 의 작성 예시다. 새 도메인을 추가할 때 이 형태를 복제해
 * 자기 대역을 하나 배정받아 쓴다.
 */
@Getter
public enum MemberErrorCode implements ErrorCodeSpec {

    MEMBER_NOT_FOUND("3000", HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."),
    DUPLICATE_LOGIN_ID("3001", HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    MemberErrorCode(String code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
