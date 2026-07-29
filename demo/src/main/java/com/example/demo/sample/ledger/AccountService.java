package com.example.demo.sample.ledger;

import com.example.demo.common.audit.Auditable;
import com.example.demo.common.error.BusinessException;
import com.example.demo.common.response.PageRequestDto;
import com.example.demo.common.response.PageResponse;
import com.example.demo.common.trace.TraceContext;
import com.example.demo.sample.ledger.dto.AccountResponse;
import com.example.demo.sample.ledger.dto.TransferRequest;
import com.example.demo.sample.ledger.dto.TransferResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 계좌 서비스.
 *
 * <p>{@link #transfer} 가 이 베이스의 트랜잭션 참조 구현이다. 실제 자금 이동 로직을 얹을 때
 * 아래 네 가지 규칙을 그대로 따르면 된다.
 *
 * <ol>
 *   <li><b>단일 트랜잭션</b> — 출금·입금·내역적재는 전부 성공하거나 전부 롤백된다.</li>
 *   <li><b>행 잠금 순서 고정</b> — 두 계좌를 계좌번호 사전순으로 잠근다.
 *       A→B 와 B→A 가 동시에 들어와도 교착(deadlock)이 생기지 않는다.</li>
 *   <li><b>잔액 갱신은 DB 에서 계산</b> — {@code balance = balance + ?} 로 갱신해
 *       읽기-수정-쓰기 사이의 갱신 손실을 없앤다.</li>
 *   <li><b>다중 방어선</b> — 애플리케이션 잔액 검증 + DB CHECK 제약. 어느 한쪽이 뚫려도 음수 잔액은 안 생긴다.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    /** 1회 이체 한도 */
    private static final BigDecimal TRANSFER_LIMIT = new BigDecimal("5000000");

    /** 이체 수수료 — 실제로는 수수료 정책 테이블에서 조회한다 */
    private static final BigDecimal TRANSFER_FEE = BigDecimal.ZERO;

    private final AccountMapper accountMapper;

    /** 내 계좌 목록 조회. */
    public List<AccountResponse> getMyAccounts(Long memberId) {
        List<Account> accounts = accountMapper.selectByMemberId(memberId);
        List<AccountResponse> responses = new ArrayList<AccountResponse>(accounts.size());
        for (Account account : accounts) {
            responses.add(AccountResponse.from(account));
        }
        return responses;
    }

    /** 계좌 단건 조회 — 본인 계좌만 허용. */
    public AccountResponse getAccount(Long memberId, String accountNo) {
        Account account = accountMapper.selectByAccountNo(accountNo);
        if (account == null) {
            throw new BusinessException(LedgerErrorCode.ACCOUNT_NOT_FOUND, "accountNo=" + accountNo);
        }
        if (!account.isOwnedBy(memberId)) {
            throw new BusinessException(LedgerErrorCode.NOT_ACCOUNT_OWNER, "memberId=" + memberId);
        }
        return AccountResponse.from(account);
    }

    /** 계좌 거래내역 조회 — 본인 계좌만 허용. */
    public PageResponse<TransferHistory> getTransferHistory(Long memberId, String accountNo, PageRequestDto pageRequest) {
        Account account = accountMapper.selectByAccountNo(accountNo);
        if (account == null) {
            throw new BusinessException(LedgerErrorCode.ACCOUNT_NOT_FOUND, "accountNo=" + accountNo);
        }
        if (!account.isOwnedBy(memberId)) {
            throw new BusinessException(LedgerErrorCode.NOT_ACCOUNT_OWNER, "memberId=" + memberId);
        }

        long total = accountMapper.countTransferHistory(accountNo);
        List<TransferHistory> content = total == 0
                ? new ArrayList<TransferHistory>()
                : accountMapper.selectTransferHistory(accountNo, pageRequest.getOffset(), pageRequest.getSize());
        return PageResponse.of(pageRequest.getPage(), pageRequest.getSize(), total, content);
    }

    /**
     * 계좌 이체.
     *
     * <p>격리수준은 {@link Isolation#READ_COMMITTED}(PostgreSQL 기본)로 두고, 정합성은
     * {@code SELECT ... FOR UPDATE} 행 잠금으로 확보한다. SERIALIZABLE 은 직렬화 실패 재시도 처리를
     * 호출자가 떠안아야 해서 단순 이체에는 과하다.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Auditable(eventType = "TRANSFER", targetExpression = "fromAccountNo",
            params = {"fromAccountNo", "toAccountNo", "amount"})
    public TransferResponse transfer(Long memberId, TransferRequest request) {
        String fromAccountNo = request.getFromAccountNo();
        String toAccountNo = request.getToAccountNo();
        BigDecimal amount = request.getAmount();

        if (fromAccountNo.equals(toAccountNo)) {
            throw new BusinessException(LedgerErrorCode.SAME_ACCOUNT_TRANSFER, "accountNo=" + fromAccountNo);
        }
        if (amount.compareTo(TRANSFER_LIMIT) > 0) {
            throw new BusinessException(LedgerErrorCode.TRANSFER_LIMIT_EXCEEDED, "amount=" + amount);
        }

        // (2) 교착 방지 — 계좌번호 사전순으로 잠금 순서를 고정한다.
        boolean fromFirst = fromAccountNo.compareTo(toAccountNo) < 0;
        Account first = accountMapper.selectByAccountNoForUpdate(fromFirst ? fromAccountNo : toAccountNo);
        Account second = accountMapper.selectByAccountNoForUpdate(fromFirst ? toAccountNo : fromAccountNo);

        Account fromAccount = fromFirst ? first : second;
        Account toAccount = fromFirst ? second : first;

        validateTransfer(memberId, fromAccount, toAccount, amount);

        // (3) 잔액 계산은 DB 에 맡긴다.
        accountMapper.updateBalance(fromAccountNo, amount.negate());
        accountMapper.updateBalance(toAccountNo, amount);

        TransferHistory history = TransferHistory.builder()
                .traceId(TraceContext.getTraceId())
                .fromAccountNo(fromAccountNo)
                .toAccountNo(toAccountNo)
                .amount(amount)
                .fee(TRANSFER_FEE)
                .currency(fromAccount.getCurrency())
                .status(TransferHistory.STATUS_COMPLETED)
                .memo(request.getMemo())
                .createdBy(TraceContext.getUserId())
                .build();
        accountMapper.insertTransferHistory(history);

        BigDecimal balanceAfter = fromAccount.getBalance().subtract(amount);
        log.info("[TRANSFER] completed. from={} amount={} balanceAfter={}",
                fromAccountNo, amount, balanceAfter);

        return TransferResponse.of(history, balanceAfter);
    }

    private void validateTransfer(Long memberId, Account fromAccount, Account toAccount, BigDecimal amount) {
        if (fromAccount == null) {
            throw new BusinessException(LedgerErrorCode.ACCOUNT_NOT_FOUND, "from account not found");
        }
        if (toAccount == null) {
            throw new BusinessException(LedgerErrorCode.ACCOUNT_NOT_FOUND, "to account not found");
        }
        if (!fromAccount.isOwnedBy(memberId)) {
            throw new BusinessException(LedgerErrorCode.NOT_ACCOUNT_OWNER, "memberId=" + memberId);
        }
        if (!fromAccount.isActive()) {
            throw new BusinessException(LedgerErrorCode.ACCOUNT_NOT_ACTIVE, "from status=" + fromAccount.getStatus());
        }
        if (!toAccount.isActive()) {
            throw new BusinessException(LedgerErrorCode.ACCOUNT_NOT_ACTIVE, "to status=" + toAccount.getStatus());
        }
        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            throw new BusinessException(LedgerErrorCode.CURRENCY_MISMATCH,
                    fromAccount.getCurrency() + "->" + toAccount.getCurrency());
        }
        // (4) 1차 방어선 — DB CHECK 제약이 2차 방어선이다.
        if (!fromAccount.hasBalanceFor(amount)) {
            throw new BusinessException(LedgerErrorCode.INSUFFICIENT_BALANCE, "requested=" + amount);
        }
    }
}
