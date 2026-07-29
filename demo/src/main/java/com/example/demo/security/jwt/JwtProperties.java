package com.example.demo.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정 ({@code app.jwt.*}).
 *
 * <p>{@link #secret} 은 Base64 로 인코딩된 32바이트 이상 키여야 한다(HS256 요구사항).
 * 운영에서는 반드시 환경변수/시크릿 매니저로 주입하고 설정 파일에 평문으로 두지 않는다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** Base64 인코딩된 서명 키 */
    private String secret;

    /** 토큰 발급자 — 검증 시 일치 여부를 확인한다 */
    private String issuer = "demo-api";

    /** 액세스 토큰 유효기간(초) */
    private long accessTokenValiditySeconds = 1800L;

    /** 리프레시 토큰 유효기간(초) */
    private long refreshTokenValiditySeconds = 43200L;

    /** 토큰을 실어 보내는 헤더명 */
    private String header = "Authorization";

    /** 토큰 접두사 */
    private String prefix = "Bearer ";
}
