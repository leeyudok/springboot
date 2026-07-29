package com.example.demo.security.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

/**
 * 인증된 주체 정보.
 *
 * <p>{@code SecurityContext} 의 principal 로 들어가며, 컨트롤러에서
 * {@code @AuthenticationPrincipal AuthenticatedMember member} 로 받아 쓴다.
 * 비밀번호·주민번호 등 민감정보는 담지 않는다.
 */
@Getter
@AllArgsConstructor
public class AuthenticatedMember implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long memberId;
    private final String loginId;
    private final String role;

    @Override
    public String toString() {
        return loginId;
    }
}
