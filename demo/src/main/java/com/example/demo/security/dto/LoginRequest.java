package com.example.demo.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 로그인 요청.
 */
@Getter
@Setter
@Schema(description = "로그인 요청")
public class LoginRequest {

    @Schema(description = "로그인 ID", example = "user01", required = true)
    @NotBlank(message = "아이디를 입력해 주세요.")
    @Size(max = 50, message = "아이디는 50자 이하여야 합니다.")
    private String loginId;

    @Schema(description = "비밀번호", example = "User1234!", required = true)
    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Size(max = 100, message = "비밀번호는 100자 이하여야 합니다.")
    private String password;

    /** 로그에 실려도 비밀번호가 노출되지 않도록 재정의한다. */
    @Override
    public String toString() {
        return "LoginRequest(loginId=" + loginId + ", password=****)";
    }
}
