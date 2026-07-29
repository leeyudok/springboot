package com.example.demo.domain.account;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 계좌 ({@code account} 테이블 매핑).
 *
 * <p>금액은 반드시 {@link BigDecimal} 로 다룬다. {@code double} 은 0.1 조차 정확히 표현하지 못해
 * 원장 대사(reconciliation)에서 차액을 만든다.
 */
@Getter
@Setter
@NoArgsConstructor
public class Account {

    /** 상태 — 정상 */
    public static final String STATUS_ACTIVE = "ACTIVE";
    /** 상태 — 거래정지 */
    public static final String STATUS_SUSPENDED = "SUSPENDED";
    /** 상태 — 해지 */
    public static final String STATUS_CLOSED = "CLOSED";

    private Long accountId;
    private String accountNo;
    private Long memberId;
    private String accountName;
    private BigDecimal balance;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    /** 거래 가능한 상태인지. */
    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    /** 해당 회원의 계좌인지. */
    public boolean isOwnedBy(Long candidateMemberId) {
        return memberId != null && memberId.equals(candidateMemberId);
    }

    /** 출금 가능 잔액인지. */
    public boolean hasBalanceFor(BigDecimal amount) {
        return balance != null && balance.compareTo(amount) >= 0;
    }
}
