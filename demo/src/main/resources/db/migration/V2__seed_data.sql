-- =============================================================
-- V2: 개발/테스트용 시드 데이터
--   비밀번호(BCrypt): admin = Passw0rd! / user01, user02 = User1234!
--   ※ 운영 배포 전 반드시 제거하거나 Flyway locations 에서 분리할 것
-- =============================================================

INSERT INTO member (login_id, password, member_name, email, cell_no, role, status, created_by) VALUES
  ('admin',  '$2y$10$lb8SkW.2ulFI7fzUOGiIkOshuABIHukRdUHmGGOgtxnMySAN0YhrW', '관리자',   'admin@example.com',  '010-0000-0001', 'ROLE_ADMIN',   'ACTIVE', 'SYSTEM'),
  ('user01', '$2y$10$X.GT2lIMV7eZ50B8Usn1eeVUmoe0oDLfwnugmtC8zMgBCAl9oxK6u', '홍길동',   'user01@example.com', '010-1234-5678', 'ROLE_USER',    'ACTIVE', 'SYSTEM'),
  ('user02', '$2y$10$X.GT2lIMV7eZ50B8Usn1eeVUmoe0oDLfwnugmtC8zMgBCAl9oxK6u', '김영희',   'user02@example.com', '010-2345-6789', 'ROLE_USER',    'ACTIVE', 'SYSTEM'),
  ('mgr01',  '$2y$10$X.GT2lIMV7eZ50B8Usn1eeVUmoe0oDLfwnugmtC8zMgBCAl9oxK6u', '박부장',   'mgr01@example.com',  '010-3456-7890', 'ROLE_MANAGER', 'ACTIVE', 'SYSTEM');

INSERT INTO account (account_no, member_id, account_name, balance, currency, status, created_by)
SELECT '110-0001-000001', m.member_id, '주거래통장', 1000000.00, 'KRW', 'ACTIVE', 'SYSTEM' FROM member m WHERE m.login_id = 'user01'
UNION ALL
SELECT '110-0001-000002', m.member_id, '비상금통장',  500000.00, 'KRW', 'ACTIVE', 'SYSTEM' FROM member m WHERE m.login_id = 'user01'
UNION ALL
SELECT '110-0002-000001', m.member_id, '급여통장',   2000000.00, 'KRW', 'ACTIVE', 'SYSTEM' FROM member m WHERE m.login_id = 'user02';
