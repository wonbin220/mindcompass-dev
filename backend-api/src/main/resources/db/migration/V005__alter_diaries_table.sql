-- 파일: V005__alter_diaries_table.sql
-- 역할: diaries 테이블을 신규 ERD에 맞게 변경
-- 관련 화면: 일기 작성, 일기 상세, 캘린더

ALTER TABLE diaries RENAME COLUMN diary_date TO written_at;
ALTER TABLE diaries ALTER COLUMN written_at TYPE TIMESTAMP USING written_at::timestamp;
ALTER TABLE diaries RENAME COLUMN emotion_score TO emotion_intensity;
ALTER TABLE diaries ALTER COLUMN emotion_intensity TYPE INTEGER USING ROUND(emotion_intensity);
ALTER TABLE diaries DROP COLUMN summary;
ALTER TABLE diaries DROP COLUMN risk_score;
ALTER TABLE diaries DROP COLUMN is_analyzed;
ALTER TABLE diaries ADD COLUMN deleted_at TIMESTAMP;
