package com.example.demo.common.audit;

import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 감사로그 보존기간 정리 잡.
 *
 * <p>{@link DisallowConcurrentExecution} — 이전 회차가 아직 돌고 있으면 겹쳐 실행하지 않는다.
 * 삭제 대상이 많아 한 회차가 길어졌을 때 같은 행을 두고 여러 인스턴스가 경합하는 것을 막는다.
 *
 * <p>잡 클래스는 Quartz 가 인스턴스를 만들지만, 스프링 부트가 붙여 주는 잡 팩토리가
 * 의존성을 주입해 주므로 {@link Autowired} 가 동작한다.
 */
@Slf4j
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class AuditLogRetentionJob implements Job {

    /** 잡 이름 */
    public static final String JOB_NAME = "auditLogRetentionJob";
    /** 잡 그룹 */
    public static final String JOB_GROUP = "maintenance";

    @Autowired
    private AuditLogRetentionService auditLogRetentionService;

    @Override
    public void execute(JobExecutionContext context) {
        try {
            auditLogRetentionService.purgeExpired();
        } catch (Exception e) {
            // 잡 실패가 스케줄러를 멈추게 두지 않는다 — 다음 회차에 다시 시도한다.
            log.error("[AUDIT-RETENTION] job failed", e);
        }
    }
}
