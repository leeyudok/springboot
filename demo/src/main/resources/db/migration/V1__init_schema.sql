-- =============================================================
-- V1: 코어 스키마 (회원 + 감사로그)
--  - 공통 컬럼 규약: created_at / created_by / updated_at / updated_by
--  - 샘플(원장) 스키마는 V2 에 분리되어 있다 — 새 프로젝트는 V2/V4 를 삭제한다
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
    reg_no        VARCHAR(200),                         -- 주민등록번호 등 고유식별정보: 애플리케이션 암호화 저장 전제
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

COMMENT ON TABLE  member                IS '회원';
COMMENT ON COLUMN member.reg_no         IS '고유식별정보(암호화 저장)';
COMMENT ON COLUMN member.login_fail_cnt IS '연속 로그인 실패 횟수';

-- ---------------------------------------------------------------
-- 감사 로그 (누가 / 언제 / 어디서 / 무엇을 / 결과)
-- ---------------------------------------------------------------
CREATE TABLE audit_log (
    audit_id      BIGSERIAL     PRIMARY KEY,
    trace_id      VARCHAR(40),
    event_type    VARCHAR(50)   NOT NULL,
    actor         VARCHAR(50),                            -- 수행 주체 (login_id, 미인증이면 ANONYMOUS)
    client_ip     VARCHAR(45),                            -- IPv6 대응 45자
    http_method   VARCHAR(10),
    request_uri   VARCHAR(500),
    target_id     VARCHAR(100),                           -- 대상 식별자
    params        TEXT,                                   -- 화이트리스트 필드만, 마스킹 적용
    result        VARCHAR(20)   NOT NULL,
    error_code    VARCHAR(50),
    error_message VARCHAR(500),
    elapsed_ms    BIGINT,
    created_at    TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT ck_audit_result CHECK (result IN ('SUCCESS', 'FAILURE'))
);

-- 보존기간 정리 배치가 created_at 범위로 지우므로 이 인덱스가 삭제 성능을 좌우한다.
CREATE INDEX ix_audit_created ON audit_log (created_at DESC);
CREATE INDEX ix_audit_actor   ON audit_log (actor, created_at DESC);
CREATE INDEX ix_audit_trace   ON audit_log (trace_id);

COMMENT ON TABLE  audit_log        IS '감사 로그';
COMMENT ON COLUMN audit_log.params IS '@Auditable 화이트리스트 필드만 마스킹 후 저장';
