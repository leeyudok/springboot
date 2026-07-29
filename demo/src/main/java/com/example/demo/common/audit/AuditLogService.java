package com.example.demo.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 감사 로그 적재 서비스.
 *
 * <p>{@link Propagation#REQUIRES_NEW} 로 <b>독립 트랜잭션</b>을 쓴다.
 * 업무 트랜잭션이 롤백되어도 "실패한 시도가 있었다"는 사실은 남아야 하기 때문이다.
 *
 * <p>적재 실패가 업무 거래를 막아서는 안 되므로 예외는 삼키고 로그만 남긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditLog auditLog) {
        try {
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            // 감사 적재 실패는 별도 모니터링 대상 — 업무 흐름은 그대로 진행시킨다.
            log.error("[AUDIT] failed to persist audit log. eventType={} actor={}",
                    auditLog.getEventType(), auditLog.getActor(), e);
        }
    }
}
