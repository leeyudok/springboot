package com.example.demo.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 토큰 발급 응답.
 */
@Getter
@Builder
@Schema(description = "토큰 발급 응답")
public class TokenResponse {

    @Schema(description = "토큰 타입", example = "Bearer")
    private final String tokenType;

    @Schema(description = "액세스 토큰")
    private final String accessToken;

    @Schema(description = "리프레시 토큰")
    private final String refreshToken;

    @Schema(description = "액세스 토큰 유효기간(초)", example = "1800")
    private final long expiresIn;

    @Schema(description = "로그인 ID", example = "user01")
    private final String loginId;

    @Schema(description = "권한", example = "ROLE_USER")
    private final String role;
}
