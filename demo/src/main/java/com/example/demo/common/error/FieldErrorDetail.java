package com.example.demo.common.error;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 필드 단위 검증 오류 상세.
 *
 * <p>{@code rejectedValue} 는 마스킹을 거쳐 담는다 — 검증 실패한 주민번호가 그대로 응답에 실리면 안 된다.
 */
@Getter
@AllArgsConstructor
@Schema(description = "필드 검증 오류")
public class FieldErrorDetail {

    @Schema(description = "필드명", example = "amount")
    private final String field;

    @Schema(description = "거부된 값 (마스킹 적용)", example = "-1000")
    private final String rejectedValue;

    @Schema(description = "오류 사유", example = "이체 금액은 0보다 커야 합니다.")
    private final String reason;
}
