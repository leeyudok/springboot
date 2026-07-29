package com.example.demo.common.audit;

import com.example.demo.common.config.AsyncConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 감사 로그 적재 서비스.
 *
 * <p>두 가지 격리를 건다.
 * <ul>
 *   <li>{@link Async} — 요청 스레드에서 떼어낸다. 감사 적재의 DB 왕복이 업무 응답시간에 얹히지 않는다.
 *       실행기는 큐가 차면 호출 스레드가 대신 실행하는 정책이라 기록이 유실되지는 않는다
 *       ({@link AsyncConfig#auditExecutor()}).</li>
 *   <li>{@link Propagation#REQUIRES_NEW} — 독립 트랜잭션. 업무 트랜잭션이 롤백되어도
 *       "실패한 시도가 있었다"는 사실은 남는다.</li>
 * </ul>
 *
 * <p>{@link AuditLog} 는 요청 스레드에서 이미 완성해 넘겨받는다. 이 메서드가 도는 시점에는
 * MDC·요청 컨텍스트가 비어 있으므로 여기서 추적 정보를 읽으려 해서는 안 된다.
 *
 * <p>적재 실패가 업무 처리를 막아서는 안 되므로 예외는 삼키고 로그만 남긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    @Async(AsyncConfig.AUDIT_EXECUTOR)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditLog auditLog) {
        try {
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            // 감사 적재 실패는 별도 모니터링 대상 — 업무 흐름은 그대로 진행시킨다.
            log.error("[AUDIT] failed to persist audit log. eventType={} actor={} traceId={}",
                    auditLog.getEventType(), auditLog.getActor(), auditLog.getTraceId(), e);
        }
    }
}
