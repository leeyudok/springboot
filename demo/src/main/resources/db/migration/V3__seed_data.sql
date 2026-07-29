-- =============================================================
-- V3: 개발/테스트용 회원 시드
--   비밀번호(BCrypt): admin = Passw0rd! / 나머지 = User1234!
--   ※ 운영 배포 전 반드시 제거하거나 Flyway locations 에서 분리할 것
-- =============================================================

INSERT INTO member (login_id, password, member_name, email, cell_no, role, status, created_by) VALUES
  ('admin',  '$2y$10$lb8SkW.2ulFI7fzUOGiIkOshuABIHukRdUHmGGOgtxnMySAN0YhrW', '관리자', 'admin@example.com',  '010-0000-0001', 'ROLE_ADMIN',   'ACTIVE', 'SYSTEM'),
  ('user01', '$2y$10$X.GT2lIMV7eZ50B8Usn1eeVUmoe0oDLfwnugmtC8zMgBCAl9oxK6u', '홍길동', 'user01@example.com', '010-1234-5678', 'ROLE_USER',    'ACTIVE', 'SYSTEM'),
  ('user02', '$2y$10$X.GT2lIMV7eZ50B8Usn1eeVUmoe0oDLfwnugmtC8zMgBCAl9oxK6u', '김영희', 'user02@example.com', '010-2345-6789', 'ROLE_USER',    'ACTIVE', 'SYSTEM'),
  ('mgr01',  '$2y$10$X.GT2lIMV7eZ50B8Usn1eeVUmoe0oDLfwnugmtC8zMgBCAl9oxK6u', '박부장', 'mgr01@example.com',  '010-3456-7890', 'ROLE_MANAGER', 'ACTIVE', 'SYSTEM');
