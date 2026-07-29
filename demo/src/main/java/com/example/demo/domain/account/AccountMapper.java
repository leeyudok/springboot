package com.example.demo.domain.account;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 계좌/이체 매퍼. SQL 은 {@code mapper/AccountMapper.xml} 에 있다.
 */
@Mapper
public interface AccountMapper {

    /** 회원의 계좌 목록. */
    List<Account> selectByMemberId(@Param("memberId") Long memberId);

    /** 계좌번호로 단건 조회 (잠금 없음 — 조회 전용). */
    Account selectByAccountNo(@Param("accountNo") String accountNo);

    /**
     * 계좌번호로 단건 조회 + 행 잠금 ({@code SELECT ... FOR UPDATE}).
     *
     * <p>반드시 트랜잭션 안에서 호출한다. 이체처럼 잔액을 읽고 쓰는 구간의 갱신 손실을 막는다.
     */
    Account selectByAccountNoForUpdate(@Param("accountNo") String accountNo);

    /**
     * 잔액을 증감한다.
     *
     * <p>{@code balance = balance + amount} 형태로 DB 에서 계산해 갱신 손실을 원천 차단한다.
     * 출금은 음수를 넘긴다. 잔액이 음수가 되면 CHECK 제약이 거부한다.
     *
     * @return 갱신된 행 수 (0 이면 계좌 없음)
     */
    int updateBalance(@Param("accountNo") String accountNo, @Param("amount") BigDecimal amount);

    /** 이체 내역 적재. */
    int insertTransferHistory(TransferHistory history);

    /** 계좌의 이체 내역 조회 (출금·입금 양방향). */
    List<TransferHistory> selectTransferHistory(@Param("accountNo") String accountNo,
                                                @Param("offset") int offset,
                                                @Param("size") int size);

    /** 계좌의 이체 내역 총건수. */
    long countTransferHistory(@Param("accountNo") String accountNo);
}
