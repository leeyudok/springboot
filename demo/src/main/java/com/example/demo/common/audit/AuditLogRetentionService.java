package com.example.demo.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 감사로그 보존기간 정리.
 *
 * <p>보존기간이 지난 행을 <b>작은 청크로 끊어</b> 반복 삭제한다.
 * {@code DELETE FROM audit_log WHERE created_at < ?} 한 방으로 지우면
 * <ul>
 *   <li>수백만 행이 한 트랜잭션에 묶여 롱 트랜잭션이 되고,</li>
 *   <li>그동안 잡은 락과 불어난 WAL 이 다른 처리·복제까지 밀어낸다.</li>
 * </ul>
 * 전체 소요시간은 길어져도 서비스에 영향이 없는 쪽을 택한다.
 *
 * <p>루프 자체에는 트랜잭션을 걸지 않는다({@link Propagation#NOT_SUPPORTED}).
 * 커밋 경계는 {@link AuditLogChunkDeleter} 가 청크 단위로 갖는다 —
 * 중간에 실패해도 그때까지 지운 만큼은 확정된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogRetentionService {

    private final AuditLogChunkDeleter chunkDeleter;
    private final AuditProperties auditProperties;

    /**
     * 보존기간이 지난 감사로그를 정리한다.
     *
     * @return 삭제한 행 수
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int purgeExpired() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(auditProperties.getRetentionDays());
        int chunkSize = auditProperties.getRetentionChunkSize();
        int maxRows = auditProperties.getRetentionMaxRowsPerRun();

        int deletedTotal = 0;
        while (deletedTotal < maxRows) {
            int deleted = chunkDeleter.deleteChunk(cutoff, chunkSize);
            if (deleted == 0) {
                break;
            }
            deletedTotal += deleted;
            sleepBetweenChunks();
        }

        if (deletedTotal >= maxRows) {
            // 상한에 걸렸다는 건 아직 남았다는 뜻 — 조용히 끝내면 "다 지웠다"로 오해된다.
            log.warn("[AUDIT-RETENTION] hit per-run limit. deleted={} cutoff={} (남은 분량은 다음 회차에 처리)",
                    deletedTotal, cutoff);
        } else {
            log.info("[AUDIT-RETENTION] completed. deleted={} cutoff={}", deletedTotal, cutoff);
        }
        return deletedTotal;
    }

    private void sleepBetweenChunks() {
        long delay = auditProperties.getRetentionChunkDelayMs();
        if (delay <= 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
