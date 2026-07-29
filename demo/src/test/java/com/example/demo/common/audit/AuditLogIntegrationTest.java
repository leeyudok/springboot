package com.example.demo.common.audit;

import com.example.demo.support.IntegrationTestSupport;
import com.example.demo.support.TestAuditMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 감사로그 통합테스트 — 비동기 적재, 파라미터 화이트리스트, 보존기간 정리를 검증한다.
 */
class AuditLogIntegrationTest extends IntegrationTestSupport {

    /** 비동기 적재 완료를 기다리는 최대 시간(ms) */
    private static final long AWAIT_TIMEOUT_MS = 5000L;

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired
    private TestAuditMapper testAuditMapper;

    @Autowired
    private AuditLogRetentionService auditLogRetentionService;

    @Autowired
    private AuditProperties auditProperties;

    @Test
    @DisplayName("로그인하면 감사로그가 남고, 화이트리스트 필드만 기록된다")
    void recordsLoginWithWhitelistedParamsOnly() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"user01\",\"password\":\"User1234!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String traceId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("traceId").asText();

        AuditLog audit = awaitAudit(traceId);

        assertThat(audit.getEventType()).isEqualTo("LOGIN");
        assertThat(audit.getActor()).isEqualTo("user01");
        assertThat(audit.getResult()).isEqualTo(AuditLog.RESULT_SUCCESS);
        assertThat(audit.getTargetId()).isEqualTo("user01");
        // 화이트리스트에 있는 loginId 는 남고, 없는 password 는 키조차 없다.
        assertThat(audit.getParams()).contains("loginId").contains("user01");
        assertThat(audit.getParams()).doesNotContain("password").doesNotContain("User1234!");
    }

    @Test
    @DisplayName("실패한 로그인도 감사로그에 남는다 — 에러코드와 함께")
    void recordsFailedLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"mgr01\",\"password\":\"WrongPass1!\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        String traceId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("traceId").asText();

        AuditLog audit = awaitAudit(traceId);

        assertThat(audit.getResult()).isEqualTo(AuditLog.RESULT_FAILURE);
        assertThat(audit.getErrorCode()).isEqualTo("2001");
        assertThat(audit.getActor()).isEqualTo("mgr01");
    }

    @Test
    @DisplayName("보존기간이 지난 감사로그만 청크 단위로 삭제된다")
    void purgesOnlyExpiredRows() {
        int retentionDays = auditProperties.getRetentionDays();
        // 청크가 여러 번 돌도록 청크 크기보다 많이 넣는다.
        int expiredCount = auditProperties.getRetentionChunkSize() + 5;

        for (int i = 0; i < expiredCount; i++) {
            insertAudit("PURGE_TARGET", LocalDateTime.now().minusDays(retentionDays + 10L));
        }
        insertAudit("PURGE_KEEP", LocalDateTime.now());

        long before = countByEventType("PURGE_TARGET");
        assertThat(before).isEqualTo(expiredCount);

        int deleted = auditLogRetentionService.purgeExpired();

        assertThat(deleted).isGreaterThanOrEqualTo(expiredCount);
        assertThat(countByEventType("PURGE_TARGET")).isZero();
        assertThat(countByEventType("PURGE_KEEP")).isEqualTo(1);
    }

    /** 비동기 적재라 즉시 조회되지 않는다 — 나타날 때까지 짧게 폴링한다. */
    private AuditLog awaitAudit(String traceId) {
        long deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            List<AuditLog> logs = auditLogMapper.selectByTraceId(traceId);
            if (!logs.isEmpty()) {
                return logs.get(0);
            }
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new AssertionError("감사로그가 " + AWAIT_TIMEOUT_MS + "ms 안에 적재되지 않았습니다. traceId=" + traceId);
    }

    private void insertAudit(String eventType, LocalDateTime createdAt) {
        AuditLog log = AuditLog.builder()
                .traceId("test-" + eventType)
                .eventType(eventType)
                .actor("tester")
                .result(AuditLog.RESULT_SUCCESS)
                .build();
        log.setCreatedAt(createdAt);
        testAuditMapper.insertWithCreatedAt(log);
    }

    private long countByEventType(String eventType) {
        return testAuditMapper.countByEventType(eventType);
    }
}
