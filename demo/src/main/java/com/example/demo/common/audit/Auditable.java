package com.example.demo.common.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 감사 대상 표시.
 *
 * <p>붙은 메서드의 실행 결과(성공/실패, 소요시간, 파라미터)를 {@code audit_log} 테이블에 남긴다.
 * 조회성 API 전체에 붙이면 로그가 폭증하므로, <b>자금 이동·개인정보 변경·권한 변경</b> 등
 * 사후 추적이 필요한 거래에만 붙인다.
 *
 * <pre>
 * &#64;Auditable(eventType = "TRANSFER", targetExpression = "fromAccountNo")
 * public TransferResponse transfer(TransferRequest request) { ... }
 * </pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /** 이벤트 유형 — 예: LOGIN, TRANSFER, MEMBER_CREATE */
    String eventType();

    /**
     * 대상 식별자를 뽑아낼 파라미터/프로퍼티 경로.
     *
     * <p>{@code "accountNo"} 처럼 첫 파라미터의 프로퍼티명을 주거나,
     * {@code "1"} 처럼 파라미터 인덱스를 줄 수 있다. 비우면 대상 식별자를 남기지 않는다.
     */
    String targetExpression() default "";

    /** 파라미터 전문을 남길지 여부 — 대용량 요청은 false 로 둔다. */
    boolean logParams() default true;
}
