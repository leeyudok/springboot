package com.example.demo.common.audit;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    /** 추적 ID 로 거래 전 구간 조회. */
    List<AuditLog> selectByTraceId(@Param("traceId") String traceId);
}
