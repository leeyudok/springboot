package com.example.demo.common.trace;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * 거래추적 컨텍스트.
 *
 * <p>요청 단위 추적 ID 와 인증 주체를 MDC 에 담아 로그·감사로그·응답 본문에서 공유한다.
 * 값은 {@link TraceIdFilter} 가 요청 진입 시 채우고 종료 시 반드시 비운다
 * (스레드 풀 재사용 환경에서 이전 요청 값이 남는 것을 방지).
 */
public final class TraceContext {

    /** 요청 추적 ID MDC 키 */
    public static final String TRACE_ID = "traceId";
    /** 인증 주체(로그인 ID) MDC 키 */
    public static final String USER_ID = "userId";
    /** 클라이언트 IP MDC 키 */
    public static final String CLIENT_IP = "clientIp";

    /** 미인증 요청의 주체 표기 */
    public static final String ANONYMOUS = "ANONYMOUS";

    private TraceContext() {
    }

    /** 32자리 하이픈 없는 UUID 를 추적 ID 로 생성한다. */
    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    /** 인증 주체를 반환한다. 미설정이면 {@link #ANONYMOUS}. */
    public static String getUserId() {
        String userId = MDC.get(USER_ID);
        return userId == null ? ANONYMOUS : userId;
    }

    public static void setUserId(String userId) {
        MDC.put(USER_ID, userId);
    }

    public static String getClientIp() {
        return MDC.get(CLIENT_IP);
    }

    public static void setClientIp(String clientIp) {
        MDC.put(CLIENT_IP, clientIp);
    }

    /** 요청 종료 시 MDC 를 비운다. */
    public static void clear() {
        MDC.remove(TRACE_ID);
        MDC.remove(USER_ID);
        MDC.remove(CLIENT_IP);
    }
}
