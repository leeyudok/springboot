package com.example.demo.support;

import com.example.demo.common.audit.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 테스트 전용 감사로그 매퍼.
 *
 * <p>"생성 시각을 임의로 지정해 넣기" 같은 조작은 운영 코드에 있으면 안 되므로
 * (감사로그의 시각을 애플리케이션이 바꿀 수 있으면 기록의 신뢰성이 무너진다)
 * 테스트 소스에만 둔다. SQL 은 {@code src/test/resources/mapper/TestAuditMapper.xml}.
 */
@Mapper
public interface TestAuditMapper {

    /** 생성 시각을 지정해 감사로그를 넣는다 (보존기간 정리 테스트용). */
    void insertWithCreatedAt(AuditLog auditLog);

    /** 이벤트 유형별 건수. */
    long countByEventType(@Param("eventType") String eventType);
}
