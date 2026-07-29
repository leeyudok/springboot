package com.example.demo.common.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 감사로그 청크 삭제 — 청크 1건 = 트랜잭션 1건.
 *
 * <p>{@link AuditLogRetentionService} 안에 두지 않고 <b>별도 빈으로 분리한 이유</b>:
 * 같은 빈의 메서드를 자기 자신이 호출하면(self-invocation) 스프링 프록시를 거치지 않아
 * {@code @Transactional} 이 <b>아무 효과 없이 무시된다</b>. 그러면 반복 삭제 전체가
 * 바깥 트랜잭션(또는 무트랜잭션)에 묶여 청크로 끊는 의미가 사라진다.
 */
@Component
@RequiredArgsConstructor
public class AuditLogChunkDeleter {

    private final AuditLogMapper auditLogMapper;

    /**
     * 기준 시각 이전 행을 최대 {@code chunkSize} 건 삭제하고 즉시 커밋한다.
     *
     * @return 실제 삭제된 행 수 (0 이면 더 지울 것이 없다)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteChunk(LocalDateTime cutoff, int chunkSize) {
        return auditLogMapper.deleteOlderThan(cutoff, chunkSize);
    }
}
