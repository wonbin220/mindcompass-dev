-- 파일: V009__create_new_tables.sql
-- 역할: refresh_tokens, user_settings, safety_events, ai_audit_logs 테이블 생성
-- 관련 화면: 로그인, 마이페이지, 채팅, 안전 분기

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE user_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    app_lock_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    notification_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    daily_reminder_time TIME,
    response_mode VARCHAR(30) NOT NULL DEFAULT 'BALANCED',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user_settings_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE safety_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    diary_id BIGINT,
    chat_session_id BIGINT,
    chat_message_id BIGINT,
    source_type VARCHAR(20) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    risk_score NUMERIC(10, 3),
    trigger_signals TEXT,
    action_taken VARCHAR(40) NOT NULL,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP,
    CONSTRAINT fk_safety_events_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_safety_events_diary
        FOREIGN KEY (diary_id) REFERENCES diaries(id),
    CONSTRAINT fk_safety_events_chat_session
        FOREIGN KEY (chat_session_id) REFERENCES chat_sessions(id),
    CONSTRAINT fk_safety_events_chat_message
        FOREIGN KEY (chat_message_id) REFERENCES chat_messages(id)
);

CREATE TABLE ai_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    diary_id BIGINT,
    chat_session_id BIGINT,
    chat_message_id BIGINT,
    request_type VARCHAR(30) NOT NULL,
    provider VARCHAR(50),
    model_name VARCHAR(100),
    request_payload TEXT,
    response_payload TEXT,
    fallback_used BOOLEAN NOT NULL DEFAULT FALSE,
    latency_ms INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ai_audit_logs_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ai_audit_logs_diary
        FOREIGN KEY (diary_id) REFERENCES diaries(id),
    CONSTRAINT fk_ai_audit_logs_chat_session
        FOREIGN KEY (chat_session_id) REFERENCES chat_sessions(id),
    CONSTRAINT fk_ai_audit_logs_chat_message
        FOREIGN KEY (chat_message_id) REFERENCES chat_messages(id)
);
