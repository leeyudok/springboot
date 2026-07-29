package com.example.demo.security;

import com.example.demo.common.response.ApiResponse;
import com.example.demo.security.dto.LoginRequest;
import com.example.demo.security.dto.RefreshRequest;
import com.example.demo.security.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 인증 API.
 *
 * <p>인증 없이 호출되는 유일한 업무 엔드포인트다 ({@code SecurityConfig.PUBLIC_PATHS}).
 */
@Tag(name = "인증", description = "로그인 / 토큰 재발급")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "로그인", description = "아이디/비밀번호로 액세스·리프레시 토큰을 발급받는다.")
    @SecurityRequirements   // 전역 bearer 요구사항 해제 (문서상 자물쇠 표시 제거)
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request), "로그인되었습니다.");
    }

    @Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 액세스 토큰을 다시 발급받는다.")
    @SecurityRequirements
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }
}
