package com.example.demo.common.config;

import com.example.demo.common.audit.AuditLogRetentionJob;
import com.example.demo.common.audit.AuditProperties;
import lombok.RequiredArgsConstructor;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 스케줄러 설정.
 *
 * <p>{@code @Scheduled} 대신 Quartz 를 쓰는 이유
 * <ul>
 *   <li><b>JDBC JobStore</b> — 트리거 상태가 DB 에 있어 재기동해도 스케줄이 보존되고,
 *       인스턴스를 여러 대 띄워도 한 번만 실행된다({@code isClustered}).
 *       {@code @Scheduled} 는 인스턴스마다 각자 돌아 중복 실행된다.</li>
 *   <li><b>misfire 정책</b> — 배포·장애로 실행 시각을 놓쳤을 때의 행동을 명시할 수 있다.</li>
 * </ul>
 *
 * <p>새 잡 추가 절차: {@code Job} 구현 → 여기에 {@code JobDetail} + {@code Trigger} 빈 추가 →
 * 크론을 설정 파일로 뺀다. 크론을 상수로 박지 않는 이유는 환경마다 실행 시각이 달라야 하기 때문이다.
 */
@Configuration
@RequiredArgsConstructor
public class QuartzConfig {

    private final AuditProperties auditProperties;

    @Bean
    public JobDetail auditLogRetentionJobDetail() {
        return JobBuilder.newJob(AuditLogRetentionJob.class)
                .withIdentity(AuditLogRetentionJob.JOB_NAME, AuditLogRetentionJob.JOB_GROUP)
                .withDescription("보존기간이 지난 감사로그 정리")
                .storeDurably()   // 트리거가 없어도 잡 정의를 유지 (수동 실행 가능)
                .build();
    }

    /**
     * 감사로그 정리 트리거.
     *
     * <p>misfire 정책은 {@code withMisfireHandlingInstructionDoNothing} —
     * 배포나 장애로 실행 시각을 놓쳤다면 <b>밀린 회차를 몰아서 실행하지 않고 건너뛴다</b>.
     * 정리 배치는 다음 회차에 처리해도 무방한 반면, 밀린 것을 한꺼번에 돌리면
     * 재기동 직후 가장 바쁜 시점에 대량 삭제가 겹친다.
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.audit", name = "retention-enabled", havingValue = "true", matchIfMissing = true)
    public Trigger auditLogRetentionTrigger(JobDetail auditLogRetentionJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(auditLogRetentionJobDetail)
                .withIdentity(AuditLogRetentionJob.JOB_NAME + "Trigger", AuditLogRetentionJob.JOB_GROUP)
                .withSchedule(CronScheduleBuilder.cronSchedule(auditProperties.getRetentionCron())
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }
}
