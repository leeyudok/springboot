package com.example.demo.common.audit;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 감사 로그 매퍼. SQL 은 {@code mapper/AuditLogMapper.xml} 에 있다.
 */
@Mapper
public interface AuditLogMapper {

    /** 감사 로그 1건 적재. */
    void insert(AuditLog auditLog);

    /** 주체별 최근 감사 로그 조회 (관리자 화면용). */
    List<AuditLog> selectByActor(@Param("actor") String actor,
                                 @Param("offset") int offset,
                                 @Param("size") int size);

    /** 추적 ID 로 요청 전 구간 조회. */
    List<AuditLog> selectByTraceId(@Param("traceId") String traceId);

    /**
     * 기준 시각 이전 행을 최대 {@code limit} 건 삭제한다 (보존기간 정리용).
     *
     * <p>PostgreSQL 의 {@code DELETE} 에는 {@code LIMIT} 절이 없으므로
     * PK 서브쿼리로 상한을 건다.
     *
     * @return 삭제된 행 수
     */
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);
}
