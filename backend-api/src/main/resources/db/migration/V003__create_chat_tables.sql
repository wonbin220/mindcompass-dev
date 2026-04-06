-- 파일: V003__create_chat_tables.sql
-- 역할: chat_sessions, chat_messages 테이블 생성
-- 관련 화면: 채팅 목록, 채팅 상세

-- 채팅 세션 테이블
CREATE TABLE chat_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 사용자별 세션 조회용 인덱스
CREATE INDEX idx_chat_sessions_user_id ON chat_sessions(user_id);
CREATE INDEX idx_chat_sessions_user_status ON chat_sessions(user_id, status);

-- 채팅 메시지 테이블
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,  -- USER or ASSISTANT
    content TEXT NOT NULL,

    -- 안전 관련 메타데이터
    is_safety_triggered BOOLEAN DEFAULT FALSE,
    safety_type VARCHAR(50),
    detected_emotion VARCHAR(50),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 세션별 메시지 조회용 인덱스
CREATE INDEX idx_chat_messages_session_id ON chat_messages(session_id);

COMMENT ON TABLE chat_sessions IS '채팅 세션 테이블';
COMMENT ON COLUMN chat_sessions.status IS '세션 상태: ACTIVE, CLOSED';

COMMENT ON TABLE chat_messages IS '채팅 메시지 테이블';
COMMENT ON COLUMN chat_messages.role IS '발신자: USER, ASSISTANT';
COMMENT ON COLUMN chat_messages.is_safety_triggered IS '안전 분기 트리거 여부';
COMMENT ON COLUMN chat_messages.safety_type IS '위기 유형 (null이면 일반 응답)';
