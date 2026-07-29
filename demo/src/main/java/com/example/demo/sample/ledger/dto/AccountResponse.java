package com.example.demo.sample.ledger.dto;

import com.example.demo.sample.ledger.Account;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 계좌 조회 응답.
 *
 * <p>본인 계좌 조회이므로 계좌번호는 마스킹하지 않는다. 타인에게 노출되는 화면
 * (수취인 확인 등)에서는 {@code SensitiveMasker.maskAccountNo} 를 적용한 별도 DTO 를 쓴다.
 */
@Getter
@Builder
@Schema(description = "계좌 정보")
public class AccountResponse {

    @Schema(description = "계좌 번호", example = "110-0001-000001")
    private final String accountNo;

    @Schema(description = "계좌 별칭", example = "주거래통장")
    private final String accountName;

    @Schema(description = "잔액", example = "1000000.00")
    private final BigDecimal balance;

    @Schema(description = "통화", example = "KRW")
    private final String currency;

    @Schema(description = "상태", example = "ACTIVE")
    private final String status;

    @Schema(description = "개설 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime createdAt;

    public static AccountResponse from(Account account) {
        return AccountResponse.builder()
                .accountNo(account.getAccountNo())
                .accountName(account.getAccountName())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
