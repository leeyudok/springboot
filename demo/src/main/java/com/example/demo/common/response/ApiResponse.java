package com.example.demo.common.response;

import com.example.demo.common.error.ErrorCode;
import com.example.demo.common.trace.TraceContext;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 전 API 공통 응답 봉투(envelope).
 *
 * <p>성공/실패와 무관하게 동일한 형태를 유지해 클라이언트 분기 로직을 단순화한다.
 * HTTP 상태코드는 전송 계층 결과, {@code code} 는 업무 결과를 나타낸다.
 *
 * <pre>
 * {
 *   "success": true,
 *   "code": "0000",
 *   "message": "정상 처리되었습니다.",
 *   "data": { ... },
 *   "traceId": "6f1c...",
 *   "timestamp": "2026-07-29 10:11:12.345"
 * }
 * </pre>
 */
@Getter
@JsonPropertyOrder({"success", "code", "message", "data", "traceId", "timestamp"})
@Schema(description = "공통 응답")
public class ApiResponse<T> {

    /** 정상 처리 업무 코드 */
    public static final String SUCCESS_CODE = "0000";
    /** 정상 처리 메시지 */
    public static final String SUCCESS_MESSAGE = "정상 처리되었습니다.";

    @Schema(description = "업무 처리 성공 여부", example = "true")
    private final boolean success;

    @Schema(description = "업무 코드 (정상 0000)", example = "0000")
    private final String code;

    @Schema(description = "결과 메시지", example = "정상 처리되었습니다.")
    private final String message;

    @Schema(description = "응답 데이터")
    private final T data;

    @Schema(description = "거래추적 ID", example = "6f1c9a2b4d8e4f3a9c0b1d2e3f4a5b6c")
    private final String traceId;

    @Schema(description = "응답 시각", example = "2026-07-29 10:11:12.345")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private final LocalDateTime timestamp;

    private ApiResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = TraceContext.getTraceId();
        this.timestamp = LocalDateTime.now();
    }

    /** 데이터를 담은 성공 응답. */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<T>(true, SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    /** 데이터 없는 성공 응답 (등록/삭제 등). */
    public static ApiResponse<Void> success() {
        return new ApiResponse<Void>(true, SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    /** 메시지를 지정한 성공 응답. */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<T>(true, SUCCESS_CODE, message, data);
    }

    /** 에러코드 기본 메시지를 사용하는 실패 응답. */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<T>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }

    /** 메시지를 재정의한 실패 응답. */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<T>(false, errorCode.getCode(), message, null);
    }

    /** 상세 정보(검증 오류 목록 등)를 함께 담은 실패 응답. */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message, T data) {
        return new ApiResponse<T>(false, errorCode.getCode(), message, data);
    }
}
