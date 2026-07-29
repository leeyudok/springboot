package com.example.demo.sample.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 이체 요청.
 *
 * <p>금액은 {@link BigDecimal} 로 받는다. {@code @Digits} 로 소수 자릿수를 제한해
 * DB 컬럼({@code numeric(18,2)})과 정밀도를 일치시킨다.
 */
@Getter
@Setter
@Schema(description = "이체 요청")
public class TransferRequest {

    @Schema(description = "출금 계좌번호", example = "110-0001-000001", required = true)
    @NotBlank(message = "출금 계좌번호는 필수입니다.")
    @Pattern(regexp = "^[0-9-]{10,30}$", message = "계좌번호 형식이 올바르지 않습니다.")
    private String fromAccountNo;

    @Schema(description = "입금 계좌번호", example = "110-0002-000001", required = true)
    @NotBlank(message = "입금 계좌번호는 필수입니다.")
    @Pattern(regexp = "^[0-9-]{10,30}$", message = "계좌번호 형식이 올바르지 않습니다.")
    private String toAccountNo;

    @Schema(description = "이체 금액", example = "10000", required = true)
    @NotNull(message = "이체 금액은 필수입니다.")
    @DecimalMin(value = "1", message = "이체 금액은 1 이상이어야 합니다.")
    @Digits(integer = 16, fraction = 2, message = "이체 금액 형식이 올바르지 않습니다.")
    private BigDecimal amount;

    @Schema(description = "적요", example = "생활비")
    @Size(max = 200, message = "적요는 200자 이하여야 합니다.")
    private String memo;
}
