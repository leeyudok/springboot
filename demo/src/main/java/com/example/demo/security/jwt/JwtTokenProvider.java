package com.example.demo.security.jwt;

import com.example.demo.common.error.BusinessException;
import com.example.demo.common.error.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * JWT 발급·검증기.
 *
 * <p>액세스 토큰과 리프레시 토큰을 {@code typ} 클레임으로 구분한다.
 * 리프레시 토큰으로 API 를 호출하는 우회를 막기 위함이다.
 *
 * <p><b>제약</b>: 서버측 토큰 저장소가 없어 발급된 액세스 토큰은 만료 전까지 강제 폐기할 수 없다.
 * 즉시 로그아웃/강제 세션 종료가 요건이면 Redis 기반 블랙리스트(jti 기준)를 추가해야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    /** 토큰 종류 클레임 키 */
    private static final String CLAIM_TYPE = "typ";
    /** 권한 클레임 키 */
    private static final String CLAIM_ROLE = "role";
    /** 회원 식별자 클레임 키 */
    private static final String CLAIM_MEMBER_ID = "mid";

    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    /** HS256 최소 키 길이(바이트) */
    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties jwtProperties;

    private Key signingKey;

    @PostConstruct
    void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret 은 Base64 디코딩 시 " + MIN_SECRET_BYTES + "바이트 이상이어야 합니다. (현재 "
                            + keyBytes.length + "바이트)");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /** 액세스 토큰 발급. */
    public String createAccessToken(Long memberId, String loginId, String role) {
        return create(memberId, loginId, role, TYPE_ACCESS, jwtProperties.getAccessTokenValiditySeconds());
    }

    /** 리프레시 토큰 발급. */
    public String createRefreshToken(Long memberId, String loginId, String role) {
        return create(memberId, loginId, role, TYPE_REFRESH, jwtProperties.getRefreshTokenValiditySeconds());
    }

    private String create(Long memberId, String loginId, String role, String type, long validitySeconds) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validitySeconds * 1000L);
        return Jwts.builder()
                .setIssuer(jwtProperties.getIssuer())
                .setSubject(loginId)
                .claim(CLAIM_MEMBER_ID, memberId)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, type)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰을 검증하고 클레임을 반환한다.
     *
     * @throws BusinessException 만료({@link ErrorCode#TOKEN_EXPIRED}) 또는 위조/형식오류({@link ErrorCode#TOKEN_INVALID})
     */
    public Claims parse(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .requireIssuer(jwtProperties.getIssuer())
                    .build()
                    .parseClaimsJws(token);
            return jws.getBody();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "expired at " + e.getClaims().getExpiration());
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, e.getClass().getSimpleName());
        }
    }

    /** 액세스 토큰인지 확인한다. 리프레시 토큰이면 거부. */
    public Claims parseAccessToken(String token) {
        Claims claims = parse(token);
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "not an access token");
        }
        return claims;
    }

    /** 리프레시 토큰인지 확인한다. */
    public Claims parseRefreshToken(String token) {
        Claims claims = parse(token);
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "not a refresh token");
        }
        return claims;
    }

    /** 클레임으로부터 스프링 시큐리티 인증 객체를 만든다. */
    public Authentication toAuthentication(Claims claims) {
        String loginId = claims.getSubject();
        String role = claims.get(CLAIM_ROLE, String.class);
        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        if (StringUtils.hasText(role)) {
            authorities.add(new SimpleGrantedAuthority(role));
        }
        AuthenticatedMember principal = new AuthenticatedMember(
                claims.get(CLAIM_MEMBER_ID, Number.class) == null
                        ? null : claims.get(CLAIM_MEMBER_ID, Number.class).longValue(),
                loginId,
                role);
        return new UsernamePasswordAuthenticationToken(principal, null,
                authorities.isEmpty() ? Collections.<GrantedAuthority>emptyList() : authorities);
    }

    /** 요청 헤더에서 토큰 문자열만 뽑아낸다. 없으면 null. */
    public String resolveToken(String headerValue) {
        if (!StringUtils.hasText(headerValue)) {
            return null;
        }
        String prefix = jwtProperties.getPrefix();
        if (headerValue.startsWith(prefix)) {
            return headerValue.substring(prefix.length()).trim();
        }
        return null;
    }

    public String getHeaderName() {
        return jwtProperties.getHeader();
    }

    public long getAccessTokenValiditySeconds() {
        return jwtProperties.getAccessTokenValiditySeconds();
    }
}
