package com.example.demo.common.audit;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 감사 로그 레코드 ({@code audit_log} 테이블 매핑).
 *
 * <p>"누가(actor) / 언제(createdAt) / 어디서(clientIp) / 무엇을(eventType, targetId) / 결과(result)"
 * 를 한 행으로 남긴다. 전자금융감독규정상 접근·거래 기록 보존 요건에 대응하는 최소 항목이다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    /** 결과 구분 — 성공 */
    public static final String RESULT_SUCCESS = "SUCCESS";
    /** 결과 구분 — 실패 */
    public static final String RESULT_FAILURE = "FAILURE";

    private Long auditId;
    private String traceId;
    private String eventType;
    private String actor;
    private String clientIp;
    private String httpMethod;
    private String requestUri;
    private String targetId;
    private String params;
    private String result;
    private String errorCode;
    private String errorMessage;
    private Long elapsedMs;
    private LocalDateTime createdAt;

    @Builder
    public AuditLog(String traceId, String eventType, String actor, String clientIp, String httpMethod,
                    String requestUri, String targetId, String params, String result,
                    String errorCode, String errorMessage, Long elapsedMs) {
        this.traceId = traceId;
        this.eventType = eventType;
        this.actor = actor;
        this.clientIp = clientIp;
        this.httpMethod = httpMethod;
        this.requestUri = requestUri;
        this.targetId = targetId;
        this.params = params;
        this.result = result;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.elapsedMs = elapsedMs;
    }
}
