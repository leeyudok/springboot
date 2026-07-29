package com.example.demo.domain.account.dto;

import com.example.demo.common.mask.SensitiveMasker;
import com.example.demo.domain.account.TransferHistory;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 이체 결과 응답.
 *
 * <p>입금 계좌번호는 타인 계좌일 수 있으므로 마스킹해서 내려준다.
 */
@Getter
@Builder
@Schema(description = "이체 결과")
public class TransferResponse {

    @Schema(description = "거래 추적 ID", example = "6f1c9a2b4d8e4f3a9c0b1d2e3f4a5b6c")
    private final String traceId;

    @Schema(description = "출금 계좌번호", example = "110-0001-000001")
    private final String fromAccountNo;

    @Schema(description = "입금 계좌번호 (마스킹)", example = "110*******001")
    private final String toAccountNo;

    @Schema(description = "이체 금액", example = "10000.00")
    private final BigDecimal amount;

    @Schema(description = "수수료", example = "0.00")
    private final BigDecimal fee;

    @Schema(description = "이체 후 출금계좌 잔액", example = "990000.00")
    private final BigDecimal balanceAfter;

    @Schema(description = "처리 상태", example = "COMPLETED")
    private final String status;

    @Schema(description = "처리 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime transferredAt;

    public static TransferResponse of(TransferHistory history, BigDecimal balanceAfter) {
        return TransferResponse.builder()
                .traceId(history.getTraceId())
                .fromAccountNo(history.getFromAccountNo())
                .toAccountNo(SensitiveMasker.maskAccountNo(history.getToAccountNo()))
                .amount(history.getAmount())
                .fee(history.getFee())
                .balanceAfter(balanceAfter)
                .status(history.getStatus())
                .transferredAt(history.getCreatedAt() == null ? LocalDateTime.now() : history.getCreatedAt())
                .build();
    }
}
