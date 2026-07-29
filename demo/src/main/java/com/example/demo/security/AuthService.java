package com.example.demo.security;

import com.example.demo.common.audit.Auditable;
import com.example.demo.common.error.BusinessException;
import com.example.demo.common.error.ErrorCode;
import com.example.demo.common.trace.TraceContext;
import com.example.demo.domain.member.Member;
import com.example.demo.domain.member.MemberMapper;
import com.example.demo.security.dto.LoginRequest;
import com.example.demo.security.dto.RefreshRequest;
import com.example.demo.security.dto.TokenResponse;
import com.example.demo.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 서비스.
 *
 * <p>로그인 실패 응답은 "아이디가 없음"과 "비밀번호 불일치"를 구분하지 않는다
 * ({@link ErrorCode#INVALID_CREDENTIALS} 단일). 계정 존재 여부를 알려주면
 * 아이디 목록을 수집당하는(user enumeration) 통로가 되기 때문이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginAttemptService loginAttemptService;

    /** 로그인 — 성공 시 액세스/리프레시 토큰을 발급한다. */
    @Auditable(eventType = "LOGIN", targetExpression = "loginId")
    public TokenResponse login(LoginRequest request) {
        // 감사로그·거래로그의 주체를 이 시점부터 확정한다 (실패해도 누가 시도했는지 남아야 함).
        TraceContext.setUserId(request.getLoginId());

        Member member = memberMapper.selectByLoginId(request.getLoginId());
        if (member == null) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "unknown loginId");
        }
        if (member.isLocked()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, "failCnt=" + member.getLoginFailCnt());
        }
        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE, "status=" + member.getStatus());
        }
        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            loginAttemptService.recordFailure(member.getLoginId());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "password mismatch");
        }

        loginAttemptService.recordSuccess(member.getMemberId());
        log.info("[AUTH] login success. memberId={}", member.getMemberId());
        return issueTokens(member);
    }

    /**
     * 토큰 재발급.
     *
     * <p>리프레시 토큰만으로 끝내지 않고 회원 상태를 다시 확인한다.
     * 토큰 발급 이후 계정이 잠기거나 해지됐다면 재발급을 막아야 하기 때문이다.
     */
    @Auditable(eventType = "TOKEN_REFRESH", logParams = false)
    public TokenResponse refresh(RefreshRequest request) {
        Claims claims = jwtTokenProvider.parseRefreshToken(request.getRefreshToken());
        String loginId = claims.getSubject();
        TraceContext.setUserId(loginId);

        Member member = memberMapper.selectByLoginId(loginId);
        if (member == null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "member not found");
        }
        if (member.isLocked()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, "failCnt=" + member.getLoginFailCnt());
        }
        if (!member.isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE, "status=" + member.getStatus());
        }
        return issueTokens(member);
    }

    private TokenResponse issueTokens(Member member) {
        return TokenResponse.builder()
                .tokenType("Bearer")
                .accessToken(jwtTokenProvider.createAccessToken(member.getMemberId(), member.getLoginId(), member.getRole()))
                .refreshToken(jwtTokenProvider.createRefreshToken(member.getMemberId(), member.getLoginId(), member.getRole()))
                .expiresIn(jwtTokenProvider.getAccessTokenValiditySeconds())
                .loginId(member.getLoginId())
                .role(member.getRole())
                .build();
    }
}
