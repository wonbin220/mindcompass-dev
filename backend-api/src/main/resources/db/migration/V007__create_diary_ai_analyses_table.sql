-- 파일: V007__create_diary_ai_analyses_table.sql
-- 역할: diary_ai_analyses 테이블 생성
-- 관련 화면: 일기 작성, 일기 상세

CREATE TABLE diary_ai_analyses (
    id BIGSERIAL PRIMARY KEY,
    diary_id BIGINT NOT NULL,
    primary_emotion VARCHAR(30),
    emotion_intensity INTEGER,
    summary TEXT,
    confidence NUMERIC(10, 3),
    raw_payload TEXT,
    risk_level VARCHAR(20),
    risk_score NUMERIC(10, 3),
    risk_signals TEXT,
    recommended_action VARCHAR(40),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_diary_ai_analyses_diary
        FOREIGN KEY (diary_id) REFERENCES diaries(id)
);
