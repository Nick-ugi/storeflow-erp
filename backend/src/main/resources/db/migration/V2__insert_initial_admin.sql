-- 초기 ADMIN 계정 (인증 · 시스템 관리 명세 4.4)
-- 아이디: admin / 초기 비밀번호: admin1234 — 첫 로그인 후 비밀번호를 변경한다.
INSERT INTO users (username, password, name, role_id, store_id)
VALUES ('admin',
        '$2a$10$Tskn3/dirJrZDJKKnMCz4.oRhOahOUJp.urZL.oXnxAyyZGCebmUm',
        '관리자',
        (SELECT id FROM roles WHERE role_name = 'ADMIN'),
        NULL);
