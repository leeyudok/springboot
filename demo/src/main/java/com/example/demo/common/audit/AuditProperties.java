package com.example.demo.common.audit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 감사로그 설정 ({@code app.audit.*}).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.audit")
public class AuditProperties {

    /** 감사로그 적재 사용 여부 */
    private boolean enabled = true;

    /**
     * 보존 기간(일). 이보다 오래된 행은 정리 배치가 삭제한다.
     *
     * <p>기본 1년은 범용 기준값이다. 적용 도메인의 법정 보존기간(전자금융거래 기록 5년,
     * 개인정보 접속기록 1~2년 등)에 맞춰 반드시 재검토한다.
     */
    private int retentionDays = 365;

    /**
     * 한 번에 삭제할 행 수.
     *
     * <p>한 트랜잭션으로 대량 삭제하면 롱 트랜잭션과 락 경합이 생기고 복제 지연까지 번진다.
     * 작게 끊어 반복하는 편이 전체 소요는 길어도 서비스 영향이 없다.
     */
    private int retentionChunkSize = 1000;

    /** 청크 사이 대기(ms) — DB 에 숨 쉴 틈을 준다 */
    private long retentionChunkDelayMs = 50L;

    /** 한 회차에 삭제할 최대 행 수 — 폭주 방지 상한 */
    private int retentionMaxRowsPerRun = 500_000;

    /** 정리 배치 크론 (기본: 매일 03:30) */
    private String retentionCron = "0 30 3 * * ?";

    /** 정리 배치 사용 여부 */
    private boolean retentionEnabled = true;
}
