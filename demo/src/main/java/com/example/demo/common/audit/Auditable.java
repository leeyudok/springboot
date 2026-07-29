package com.example.demo.common.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 감사 대상 표시.
 *
 * <p>붙은 메서드의 실행 결과(성공/실패, 소요시간, 지정 파라미터)를 {@code audit_log} 테이블에 남긴다.
 * 조회성 API 전체에 붙이면 로그가 폭증하므로, <b>자원 이동·개인정보 변경·권한 변경</b> 등
 * 사후 추적이 필요한 처리에만 붙인다.
 *
 * <pre>
 * &#64;Auditable(eventType = "TRANSFER", targetExpression = "fromAccountNo",
 *            params = {"fromAccountNo", "toAccountNo", "amount"})
 * public TransferResponse transfer(Long memberId, TransferRequest request) { ... }
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
     * <p>{@code "accountNo"} 처럼 파라미터명 또는 파라미터 객체의 프로퍼티명을 주거나,
     * {@code "1"} 처럼 파라미터 인덱스를 줄 수 있다. 비우면 대상 식별자를 남기지 않는다.
     */
    String targetExpression() default "";

    /**
     * {@code audit_log.params} 에 기록할 필드 <b>화이트리스트</b>.
     *
     * <p>지정한 이름만 기록하며, 비워두면 아무것도 기록하지 않는다. 표기법은
     * {@link #targetExpression()} 과 같다(파라미터명 또는 파라미터 객체의 프로퍼티명).
     *
     * <p>파라미터 전체를 직렬화한 뒤 마스킹으로 걸러내는 방식(블랙리스트)을 쓰지 않는 이유 —
     * 마스킹 패턴에 걸리지 않는 새로운 형태의 비밀값(API 키, 일회용 인증번호, 내부 토큰)이
     * 추가되는 순간 그대로 평문 적재되기 때문이다. 기록할 것을 고르는 편이 안전하다.
     *
     * <p>화이트리스트로 뽑은 값에도 마스킹은 그대로 적용된다(이중 방어).
     */
    String[] params() default {};
}
