package com.example.demo.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

/**
 * 토큰 재발급 요청.
 */
@Getter
@Setter
@Schema(description = "토큰 재발급 요청")
public class RefreshRequest {

    @Schema(description = "리프레시 토큰", required = true)
    @NotBlank(message = "리프레시 토큰은 필수입니다.")
    private String refreshToken;

    @Override
    public String toString() {
        return "RefreshRequest(refreshToken=****)";
    }
}
