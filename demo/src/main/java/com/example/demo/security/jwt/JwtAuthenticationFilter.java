package com.example.demo.security.jwt;

import com.example.demo.common.error.BusinessException;
import com.example.demo.common.trace.TraceContext;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT 인증 필터.
 *
 * <p>{@code Authorization: Bearer <token>} 을 검증해 {@code SecurityContext} 를 채우고,
 * 인증 주체를 MDC 에 넣어 이후 모든 로그·감사로그가 "누가" 한 요청인지 갖도록 한다.
 *
 * <p>토큰이 없거나 잘못됐어도 여기서 응답을 만들지 않는다. 인증 없는 상태로 체인을 계속 태우고
 * 최종 판단은 {@code SecurityConfig} 의 인가 규칙 →
 * {@link RestAuthenticationEntryPoint} 가 하도록 위임한다. 실패 사유는 요청 속성에 실어 보낸다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 인증 실패 사유를 EntryPoint 로 전달하는 요청 속성 키 */
    public static final String ATTR_AUTH_ERROR = "app.auth.error";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = jwtTokenProvider.resolveToken(request.getHeader(jwtTokenProvider.getHeaderName()));
        if (token != null) {
            try {
                Claims claims = jwtTokenProvider.parseAccessToken(token);
                Authentication authentication = jwtTokenProvider.toAuthentication(claims);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                TraceContext.setUserId(claims.getSubject());
            } catch (BusinessException e) {
                // 토큰이 유효하지 않으면 인증 없는 상태로 진행 — 인가 단계에서 401 로 정리된다.
                SecurityContextHolder.clearContext();
                request.setAttribute(ATTR_AUTH_ERROR, e.getErrorCode());
                log.debug("[AUTH] token rejected: {}", e.getErrorCode().getCode());
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
