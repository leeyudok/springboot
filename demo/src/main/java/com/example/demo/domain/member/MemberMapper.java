package com.example.demo.domain.member;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 회원 매퍼. SQL 은 {@code mapper/MemberMapper.xml} 에 있다.
 */
@Mapper
public interface MemberMapper {

    /** 로그인 ID 로 회원 1건 조회 (인증용 — 비밀번호 포함). */
    Member selectByLoginId(@Param("loginId") String loginId);

    /** PK 로 회원 1건 조회. */
    Member selectById(@Param("memberId") Long memberId);

    /**
     * 회원 목록 조회 (이름 부분일치 + 상태 필터).
     *
     * @param sort 정렬 키 — 매퍼 XML 의 허용 목록에 없는 값은 기본 정렬로 처리된다
     */
    List<Member> selectList(@Param("memberName") String memberName,
                            @Param("status") String status,
                            @Param("sort") String sort,
                            @Param("offset") int offset,
                            @Param("size") int size);

    /** 회원 목록 총건수. */
    long countList(@Param("memberName") String memberName,
                   @Param("status") String status);

    /** 로그인 ID 중복 건수. */
    int countByLoginId(@Param("loginId") String loginId);

    /** 회원 등록. 채번된 PK 는 파라미터 객체에 채워진다. */
    int insert(Member member);

    /** 회원 정보 수정 (이름/이메일/휴대폰). */
    int update(Member member);

    /** 로그인 성공 처리 — 최종 로그인 시각 갱신 및 실패 횟수 초기화. */
    int updateLoginSuccess(@Param("memberId") Long memberId);

    /** 로그인 실패 처리 — 실패 횟수 증가, 임계 초과 시 상태를 LOCKED 로 전환. */
    int updateLoginFailure(@Param("loginId") String loginId,
                           @Param("maxFailCount") int maxFailCount);
}
