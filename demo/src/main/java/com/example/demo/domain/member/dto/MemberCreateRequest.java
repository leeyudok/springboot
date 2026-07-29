package com.example.demo.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 회원 등록 요청.
 *
 * <p>비밀번호 규칙은 전자금융감독규정 권고(영문·숫자·특수문자 조합 8자 이상)를 따른다.
 */
@Getter
@Setter
@Schema(description = "회원 등록 요청")
public class MemberCreateRequest {

    @Schema(description = "로그인 ID (영문 소문자·숫자 4~20자)", example = "user03", required = true)
    @NotBlank(message = "로그인 ID는 필수입니다.")
    @Pattern(regexp = "^[a-z0-9]{4,20}$", message = "로그인 ID는 영문 소문자와 숫자 4~20자여야 합니다.")
    private String loginId;

    @Schema(description = "비밀번호 (영문·숫자·특수문자 조합 8~20자)", example = "User1234!", required = true)
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,20}$",
            message = "비밀번호는 영문·숫자·특수문자를 모두 포함한 8~20자여야 합니다.")
    private String password;

    @Schema(description = "회원명", example = "홍길동", required = true)
    @NotBlank(message = "회원명은 필수입니다.")
    @Size(max = 100, message = "회원명은 100자 이하여야 합니다.")
    private String memberName;

    @Schema(description = "이메일", example = "user03@example.com")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 150, message = "이메일은 150자 이하여야 합니다.")
    private String email;

    @Schema(description = "휴대폰 번호", example = "010-1234-5678")
    @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
    private String cellNo;
}
