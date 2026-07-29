package com.example.demo.domain.account;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 이체 거래 내역 ({@code transfer_history} 테이블 매핑).
 */
@Getter
@Setter
@NoArgsConstructor
public class TransferHistory {

    /** 상태 — 정상 완료 */
    public static final String STATUS_COMPLETED = "COMPLETED";
    /** 상태 — 실패 */
    public static final String STATUS_FAILED = "FAILED";
    /** 상태 — 취소 */
    public static final String STATUS_CANCELED = "CANCELED";

    private Long transferId;
    private String traceId;
    private String fromAccountNo;
    private String toAccountNo;
    private BigDecimal amount;
    private BigDecimal fee;
    private String currency;
    private String status;
    private String memo;
    private LocalDateTime createdAt;
    private String createdBy;

    @Builder
    public TransferHistory(String traceId, String fromAccountNo, String toAccountNo, BigDecimal amount,
                           BigDecimal fee, String currency, String status, String memo, String createdBy) {
        this.traceId = traceId;
        this.fromAccountNo = fromAccountNo;
        this.toAccountNo = toAccountNo;
        this.amount = amount;
        this.fee = fee;
        this.currency = currency;
        this.status = status;
        this.memo = memo;
        this.createdBy = createdBy;
    }
}
