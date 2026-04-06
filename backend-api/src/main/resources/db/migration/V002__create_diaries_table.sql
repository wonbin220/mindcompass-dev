-- 파일: V002__create_diaries_table.sql
-- 역할: diaries 테이블 생성
-- 관련 화면: 일기 작성, 일기 상세, 캘린더

CREATE TABLE diaries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    diary_date DATE NOT NULL,

    -- AI 분석 결과 (nullable - AI 실패해도 저장됨)
    primary_emotion VARCHAR(50),
    emotion_score DOUBLE PRECISION,
    summary VARCHAR(500),
    risk_score INTEGER,
    is_analyzed BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 사용자별 일기 조회용 인덱스
CREATE INDEX idx_diaries_user_id ON diaries(user_id);

-- 날짜별 조회용 인덱스
CREATE INDEX idx_diaries_user_date ON diaries(user_id, diary_date);

-- 감정별 조회용 인덱스
CREATE INDEX idx_diaries_emotion ON diaries(user_id, primary_emotion);

-- 미분석 일기 조회용 인덱스 (배치 처리)
CREATE INDEX idx_diaries_unanalyzed ON diaries(is_analyzed) WHERE is_analyzed = FALSE;

COMMENT ON TABLE diaries IS '감정 일기 테이블';
COMMENT ON COLUMN diaries.diary_date IS '일기가 기록하는 날짜 (작성일과 다를 수 있음)';
COMMENT ON COLUMN diaries.is_analyzed IS 'AI 분석 완료 여부';
COMMENT ON COLUMN diaries.risk_score IS '위험도 점수 0-100';
