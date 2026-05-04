-- 파일: V004__alter_users_table.sql
-- 역할: users 테이블을 신규 ERD에 맞게 변경
-- 관련 화면: 회원가입, 로그인, 마이페이지

ALTER TABLE users RENAME COLUMN name TO nickname;
ALTER TABLE users RENAME COLUMN password TO password_hash;
ALTER TABLE users DROP COLUMN profile_image_url;
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP;
