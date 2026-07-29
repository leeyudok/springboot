-- =============================================================
-- V2: 원장(ledger) 샘플 스키마
--   com.example.demo.sample.ledger 참조 구현 전용.
--   ※ 새 프로젝트는 이 파일과 V4 를 삭제한다.
-- =============================================================

CREATE TABLE account (
    account_id    BIGSERIAL     PRIMARY KEY,
    account_no    VARCHAR(30)   NOT NULL,
    member_id     BIGINT        NOT NULL,
    account_name  VARCHAR(100),
    balance       NUMERIC(18,2) NOT NULL DEFAULT 0,
    currency      CHAR(3)       NOT NULL DEFAULT 'KRW',
    status        VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP     NOT NULL DEFAULT now(),
    created_by    VARCHAR(50),
    updated_at    TIMESTAMP,
    updated_by    VARCHAR(50),
    CONSTRAINT uk_account_no       UNIQUE (account_no),
    CONSTRAINT fk_account_member   FOREIGN KEY (member_id) REFERENCES member (member_id),
    -- 애플리케이션 검증이 뚫려도 음수 잔액은 DB 가 최종적으로 거부한다.
    CONSTRAINT ck_account_balance  CHECK (balance >= 0),
    CONSTRAINT ck_account_status   CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'))
);

CREATE INDEX ix_account_member ON account (member_id);

COMMENT ON TABLE  account         IS '원장 계정 (샘플)';
COMMENT ON COLUMN account.balance IS '잔액 — numeric 사용, 부동소수 금지';

CREATE TABLE transfer_history (
    transfer_id     BIGSERIAL     PRIMARY KEY,
    trace_id        VARCHAR(40),                         -- 요청 추적 ID (MDC 와 동일 값)
    from_account_no VARCHAR(30)   NOT NULL,
    to_account_no   VARCHAR(30)   NOT NULL,
    amount          NUMERIC(18,2) NOT NULL,
    fee             NUMERIC(18,2) NOT NULL DEFAULT 0,
    currency        CHAR(3)       NOT NULL DEFAULT 'KRW',
    status          VARCHAR(20)   NOT NULL,
    memo            VARCHAR(200),
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    created_by      VARCHAR(50),
    CONSTRAINT ck_transfer_amount CHECK (amount > 0),
    CONSTRAINT ck_transfer_status CHECK (status IN ('COMPLETED', 'FAILED', 'CANCELED')),
    CONSTRAINT ck_transfer_self   CHECK (from_account_no <> to_account_no)
);

CREATE INDEX ix_transfer_from  ON transfer_history (from_account_no, created_at DESC);
CREATE INDEX ix_transfer_to    ON transfer_history (to_account_no, created_at DESC);
CREATE INDEX ix_transfer_trace ON transfer_history (trace_id);

COMMENT ON TABLE transfer_history IS '원장 이동 내역 (샘플)';
