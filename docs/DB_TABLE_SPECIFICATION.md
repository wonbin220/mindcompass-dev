# Mind Compass DB 테이블 명세

이 문서는 Mind Compass의 데이터베이스 테이블 구조를 정리한 것이다.
Flyway 마이그레이션 파일(`backend-api/src/main/resources/db/migration/`)이 원본이고, 이 문서는 학습용 요약이다.

---

## 테이블 관계도

```
users (1) ──── (N) diaries
  │
  └──── (N) chat_sessions (1) ──── (N) chat_messages
```

---

## 1. users

> 마이그레이션: `V001__create_users_table.sql`
> 관련 화면: 회원가입, 로그인, 마이페이지

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| id | BIGSERIAL | PK | 자동 증가 식별자 |
| email | VARCHAR(100) | NOT NULL, UNIQUE | 로그인용 이메일 |
| password | VARCHAR(255) | NOT NULL | BCrypt 암호화된 비밀번호 |
| name | VARCHAR(50) | NOT NULL | 사용자 이름 |
| profile_image_url | VARCHAR(500) | nullable | 프로필 이미지 URL |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | 계정 상태: ACTIVE, INACTIVE, SUSPENDED |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW | 생성 시각 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW | 수정 시각 |

**인덱스:**
- `idx_users_email` — 이메일 검색 (로그인 시 사용)
- `idx_users_status` — 상태 필터

**설계 이유:**
- `status`를 enum이 아닌 VARCHAR로 둔 이유: DB 마이그레이션 없이 상태값 추가 가능
- `password`가 255자인 이유: BCrypt 해시 결과가 60자지만 알고리즘 변경 여지를 남김

---

## 2. diaries

> 마이그레이션: `V002__create_diaries_table.sql`
> 관련 화면: 일기 작성, 일기 상세, 캘린더

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| id | BIGSERIAL | PK | 자동 증가 식별자 |
| user_id | BIGINT | NOT NULL, FK → users(id) | 작성자 |
| title | VARCHAR(200) | NOT NULL | 일기 제목 |
| content | TEXT | NOT NULL | 일기 본문 |
| diary_date | DATE | NOT NULL | 일기가 기록하는 날짜 (작성일과 다를 수 있음) |
| primary_emotion | VARCHAR(50) | nullable | AI 분석 감정 |
| emotion_score | DOUBLE PRECISION | nullable | 감정 점수 (0.0~1.0) |
| summary | VARCHAR(500) | nullable | AI 요약 |
| risk_score | INTEGER | nullable | 위험도 점수 (0~100) |
| is_analyzed | BOOLEAN | NOT NULL, DEFAULT FALSE | AI 분석 완료 여부 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW | 생성 시각 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW | 수정 시각 |

**인덱스:**
- `idx_diaries_user_id` — 사용자별 일기 목록
- `idx_diaries_user_date` — 캘린더 날짜별 조회 (복합 인덱스)
- `idx_diaries_emotion` — 감정별 필터링 (복합 인덱스)
- `idx_diaries_unanalyzed` — 미분석 일기 배치 처리 (부분 인덱스)

**설계 이유:**
- AI 분석 컬럼이 nullable인 이유: **AI 실패해도 일기 저장은 성공해야 한다** (safety-first 원칙)
- `diary_date`와 `created_at`을 분리한 이유: 어제 일기를 오늘 쓸 수 있다
- `is_analyzed` 부분 인덱스: FALSE인 행만 인덱싱해서 배치 재분석 성능 확보

---

## 3. chat_sessions

> 마이그레이션: `V003__create_chat_tables.sql`
> 관련 화면: 채팅 목록

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| id | BIGSERIAL | PK | 자동 증가 식별자 |
| user_id | BIGINT | NOT NULL, FK → users(id) | 세션 소유자 |
| title | VARCHAR(200) | nullable | 세션 제목 (첫 메시지 기반 자동 생성 가능) |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | 세션 상태: ACTIVE, CLOSED |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW | 생성 시각 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW | 수정 시각 |

**인덱스:**
- `idx_chat_sessions_user_id` — 사용자별 세션 목록
- `idx_chat_sessions_user_status` — 활성 세션만 조회 (복합 인덱스)

---

## 4. chat_messages

> 마이그레이션: `V003__create_chat_tables.sql`
> 관련 화면: 채팅 상세

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| id | BIGSERIAL | PK | 자동 증가 식별자 |
| session_id | BIGINT | NOT NULL, FK → chat_sessions(id) ON DELETE CASCADE | 소속 세션 |
| role | VARCHAR(20) | NOT NULL | 발신자: USER, ASSISTANT |
| content | TEXT | NOT NULL | 메시지 내용 |
| is_safety_triggered | BOOLEAN | DEFAULT FALSE | 안전 분기 트리거 여부 |
| safety_type | VARCHAR(50) | nullable | 위기 유형 (null이면 일반 응답) |
| detected_emotion | VARCHAR(50) | nullable | 감지된 감정 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW | 생성 시각 |

**인덱스:**
- `idx_chat_messages_session_id` — 세션별 메시지 목록

**설계 이유:**
- `ON DELETE CASCADE`: 세션 삭제 시 메시지도 함께 삭제 (고아 레코드 방지)
- `is_safety_triggered`: 위기 감지된 메시지를 빠르게 추적/모니터링
- `safety_type`: 어떤 종류의 위기인지 기록 (자해, 자살 등 — 통계/개선용)

---

## 향후 추가 예상 테이블

| 테이블 | Phase | 용도 |
|--------|-------|------|
| reports | Phase 8 | 주간/월간 리포트 저장 |
| emotion_logs | Phase 5 | AI 분석 이력 (모델 비교용) |
| safety_events | Phase 7 | 위기 감지 이벤트 로그 |

---

## 참고

- 마이그레이션 원본 파일: `backend-api/src/main/resources/db/migration/`
- 로컬 개발 시: H2 인메모리 DB 사용 (Flyway 비활성화, JPA ddl-auto: create-drop)
- 개발/운영 시: PostgreSQL + Flyway 마이그레이션
