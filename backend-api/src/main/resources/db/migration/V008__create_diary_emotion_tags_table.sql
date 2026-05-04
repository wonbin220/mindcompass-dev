-- 파일: V008__create_diary_emotion_tags_table.sql
-- 역할: diary_emotion_tags 테이블 생성
-- 관련 화면: 일기 작성, 일기 상세

CREATE TABLE diary_emotion_tags (
    id BIGSERIAL PRIMARY KEY,
    diary_id BIGINT NOT NULL,
    emotion_code VARCHAR(30) NOT NULL,
    intensity INTEGER,
    source_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_diary_emotion_tags_diary
        FOREIGN KEY (diary_id) REFERENCES diaries(id)
);
