package com.example.demo.domain.member;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 회원 ({@code member} 테이블 매핑).
 *
 * <p>{@link #password} / {@link #regNo} 는 민감정보이므로 이 객체를 그대로 응답에 실어서는 안 된다.
 * 컨트롤러로는 반드시 {@link com.example.demo.domain.member.dto.MemberResponse} 로 변환해 내보낸다.
 */
@Getter
@Setter
@NoArgsConstructor
public class Member {

    /** 상태 — 정상 */
    public static final String STATUS_ACTIVE = "ACTIVE";
    /** 상태 — 잠김 (로그인 실패 초과) */
    public static final String STATUS_LOCKED = "LOCKED";
    /** 상태 — 휴면 */
    public static final String STATUS_DORMANT = "DORMANT";
    /** 상태 — 해지 */
    public static final String STATUS_CLOSED = "CLOSED";

    /** 계정 잠금 임계 실패 횟수 */
    public static final int MAX_LOGIN_FAIL_COUNT = 5;

    private Long memberId;
    private String loginId;
    private String password;
    private String memberName;
    private String email;
    private String cellNo;
    private String regNo;
    private String role;
    private String status;
    private int loginFailCnt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    @Builder
    public Member(Long memberId, String loginId, String password, String memberName, String email,
                  String cellNo, String regNo, String role, String status, String createdBy) {
        this.memberId = memberId;
        this.loginId = loginId;
        this.password = password;
        this.memberName = memberName;
        this.email = email;
        this.cellNo = cellNo;
        this.regNo = regNo;
        this.role = role;
        this.status = status;
        this.createdBy = createdBy;
    }

    /** 로그인 가능한 상태인지. */
    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    /** 실패 횟수 초과로 잠긴 상태인지. */
    public boolean isLocked() {
        return STATUS_LOCKED.equals(status) || loginFailCnt >= MAX_LOGIN_FAIL_COUNT;
    }
}
