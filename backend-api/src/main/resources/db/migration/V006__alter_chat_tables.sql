-- 파일: V006__alter_chat_tables.sql
-- 역할: chat_sessions, chat_messages 테이블을 신규 ERD에 맞게 변경
-- 관련 화면: 채팅 목록, 채팅 상세

ALTER TABLE chat_sessions ADD COLUMN source_diary_id BIGINT;

ALTER TABLE chat_messages DROP COLUMN is_safety_triggered;
ALTER TABLE chat_messages DROP COLUMN safety_type;
ALTER TABLE chat_messages DROP COLUMN detected_emotion;
