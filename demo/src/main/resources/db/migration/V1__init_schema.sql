-- =============================================================
-- V1: 기본 스키마
--  - 공통 컬럼 규약: created_at / created_by / updated_at / updated_by
--  - 금액 컬럼은 numeric(18,2) (부동소수 사용 금지)
-- =============================================================

-- ---------------------------------------------------------------
-- 회원
-- ---------------------------------------------------------------
CREATE TABLE member (
    member_id     BIGSERIAL     PRIMARY KEY,
    login_id      VARCHAR(50)   NOT NULL,
    password      VARCHAR(100)  NOT NULL,               -- BCrypt 해시
    member_name   VARCHAR(100)  NOT NULL,
    email         VARCHAR(150),
    cell_no       VARCHAR(20),
    reg_no        VARCHAR(200),                         -- 주민등록번호: 애플리케이션 암호화 저장 전제
    role          VARCHAR(20)   NOT NULL DEFAULT 'ROLE_USER',
    status        VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    login_fail_cnt SMALLINT     NOT NULL DEFAULT 0,     -- 연속 로그인 실패 횟수 (5회 초과 시 잠금)
    last_login_at TIMESTAMP,
    created_at    TIMESTAMP     NOT NULL DEFAULT now(),
    created_by    VARCHAR(50),
    updated_at    TIMESTAMP,
    updated_by    VARCHAR(50),
    CONSTRAINT uk_member_login_id UNIQUE (login_id),
    CONSTRAINT ck_member_role   CHECK (role IN ('ROLE_USER', 'ROLE_MANAGER', 'ROLE_ADMIN')),
    CONSTRAINT ck_member_status CHECK (status IN ('ACTIVE', 'LOCKED', 'DORMANT', 'CLOSED'))
);

COMMENT ON TABLE  member              IS '회원';
COMMENT ON COLUMN member.reg_no       IS '주민등록번호(암호화 저장)';
COMMENT ON COLUMN member.login_fail_cnt IS '연속 로그인 실패 횟수';

-- ---------------------------------------------------------------
-- 계좌
-- ---------------------------------------------------------------
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
    CONSTRAINT ck_account_balance  CHECK (balance >= 0),          -- 잔액 음수 방지 (DB 최종 방어선)
    CONSTRAINT ck_account_status   CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'))
);

CREATE INDEX ix_account_member ON account (member_id);

COMMENT ON TABLE  account         IS '계좌';
COMMENT ON COLUMN account.balance IS '잔액 (numeric — 부동소수 금지)';

-- ---------------------------------------------------------------
-- 이체 거래 내역
-- ---------------------------------------------------------------
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

CREATE INDEX ix_transfer_from    ON transfer_history (from_account_no, created_at DESC);
CREATE INDEX ix_transfer_to      ON transfer_history (to_account_no, created_at DESC);
CREATE INDEX ix_transfer_trace   ON transfer_history (trace_id);

COMMENT ON TABLE transfer_history IS '이체 거래 내역';

-- ---------------------------------------------------------------
-- 감사 로그 (전자금융감독규정 대응 — 누가/언제/무엇을/결과)
-- ---------------------------------------------------------------
CREATE TABLE audit_log (
    audit_id      BIGSERIAL     PRIMARY KEY,
    trace_id      VARCHAR(40),
    event_type    VARCHAR(50)   NOT NULL,
    actor         VARCHAR(50),                            -- 수행 주체 (login_id, 미인증이면 ANONYMOUS)
    client_ip     VARCHAR(45),                            -- IPv6 대응 45자
    http_method   VARCHAR(10),
    request_uri   VARCHAR(500),
    target_id     VARCHAR(100),                           -- 대상 식별자 (계좌번호, 회원ID 등)
    params        TEXT,                                   -- 마스킹 적용된 요청 파라미터
    result        VARCHAR(20)   NOT NULL,
    error_code    VARCHAR(50),
    error_message VARCHAR(500),
    elapsed_ms    BIGINT,
    created_at    TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT ck_audit_result CHECK (result IN ('SUCCESS', 'FAILURE'))
);

CREATE INDEX ix_audit_created ON audit_log (created_at DESC);
CREATE INDEX ix_audit_actor   ON audit_log (actor, created_at DESC);
CREATE INDEX ix_audit_trace   ON audit_log (trace_id);

COMMENT ON TABLE  audit_log        IS '감사 로그';
COMMENT ON COLUMN audit_log.params IS '민감정보 마스킹 후 저장';
