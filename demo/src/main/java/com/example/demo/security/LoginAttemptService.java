package com.example.demo.security;

import com.example.demo.domain.member.Member;
import com.example.demo.domain.member.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 시도 기록 서비스.
 *
 * <p>실패 횟수 증가는 <b>독립 트랜잭션</b>({@link Propagation#REQUIRES_NEW})이어야 한다.
 * 로그인 실패는 예외로 끝나는데, 같은 트랜잭션에 묶여 있으면 롤백되면서 실패 횟수가 사라져
 * 계정 잠금이 영원히 걸리지 않는다.
 *
 * <p>별도 빈으로 분리한 이유는 자기호출(self-invocation)로는 프록시를 타지 못해
 * 전파 속성이 적용되지 않기 때문이다.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final MemberMapper memberMapper;

    /** 로그인 실패 기록 — 임계 초과 시 계정을 잠근다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String loginId) {
        memberMapper.updateLoginFailure(loginId, Member.MAX_LOGIN_FAIL_COUNT);
    }

    /** 로그인 성공 기록 — 최종 로그인 시각 갱신 및 실패 횟수 초기화. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(Long memberId) {
        memberMapper.updateLoginSuccess(memberId);
    }
}
