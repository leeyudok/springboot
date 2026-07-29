package com.example.demo.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 실행 설정.
 *
 * <p>감사로그 적재처럼 <b>업무 응답시간에 얹히면 안 되는 부가 작업</b>을 요청 스레드에서 떼어낸다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /** 감사로그 전용 실행기 빈 이름 */
    public static final String AUDIT_EXECUTOR = "auditExecutor";

    /**
     * 감사로그 전용 실행기.
     *
     * <p>거부 정책이 {@link ThreadPoolExecutor.CallerRunsPolicy} 인 것이 핵심이다.
     * 큐가 가득 차면 작업을 버리는 대신 호출 스레드가 직접 실행한다 — 느려질지언정
     * <b>감사 기록은 유실되지 않는다</b>. 감사는 규제 대응 자료라 "바쁘면 버린다"가 성립하지 않는다.
     *
     * <p>종료 시에는 큐에 남은 작업을 마저 처리하고 내려간다({@code setWaitForTasksToCompleteOnShutdown}).
     * 서버의 graceful shutdown 과 짝을 이룬다.
     */
    @Bean(name = AUDIT_EXECUTOR)
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("audit-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }
}
