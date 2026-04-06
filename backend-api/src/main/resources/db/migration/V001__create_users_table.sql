-- 파일: V001__create_users_table.sql
-- 역할: users 테이블 생성
-- 관련 화면: 회원가입, 로그인, 마이페이지

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    profile_image_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 이메일 검색용 인덱스
CREATE INDEX idx_users_email ON users(email);

-- 상태 필터용 인덱스
CREATE INDEX idx_users_status ON users(status);

COMMENT ON TABLE users IS '사용자 정보 테이블';
COMMENT ON COLUMN users.email IS '로그인용 이메일 (유니크)';
COMMENT ON COLUMN users.password IS 'BCrypt 암호화된 비밀번호';
COMMENT ON COLUMN users.status IS '계정 상태: ACTIVE, INACTIVE, SUSPENDED';
