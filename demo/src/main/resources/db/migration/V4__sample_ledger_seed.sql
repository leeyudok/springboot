-- =============================================================
-- V4: 원장 샘플 시드
--   ※ 새 프로젝트는 V2 와 함께 이 파일을 삭제한다.
-- =============================================================

INSERT INTO account (account_no, member_id, account_name, balance, currency, status, created_by)
SELECT '110-0001-000001', m.member_id, '주거래통장', 1000000.00, 'KRW', 'ACTIVE', 'SYSTEM' FROM member m WHERE m.login_id = 'user01'
UNION ALL
SELECT '110-0001-000002', m.member_id, '비상금통장',  500000.00, 'KRW', 'ACTIVE', 'SYSTEM' FROM member m WHERE m.login_id = 'user01'
UNION ALL
SELECT '110-0002-000001', m.member_id, '급여통장',   2000000.00, 'KRW', 'ACTIVE', 'SYSTEM' FROM member m WHERE m.login_id = 'user02';
