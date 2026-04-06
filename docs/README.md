# Mind Compass 학습 문서 안내

이 폴더는 Mind Compass 프로젝트의 학습 문서를 모아둔 곳이다.

---

## 문서 목록

### Backend API 학습 문서

| 문서 | 설명 |
|------|------|
| `AUTH_API_LEARNING.md` | 회원가입, 로그인, JWT 인증 |
| `DIARY_API_LEARNING.md` | 일기 저장, 조회, 수정, 삭제 |
| `CALENDAR_API_LEARNING.md` | 날짜별/기간별 일기 조회 |
| `CHAT_API_LEARNING.md` | 채팅 세션, 메시지 저장, AI 답변 |
| `REPORT_API_LEARNING.md` | 주간/월간 감정 리포트 |

### 참조 문서

| 문서 | 설명 |
|------|------|
| `DB_TABLE_SPECIFICATION.md` | 테이블 구조, 관계, 컬럼 명세 |
| `SCREEN_TO_API_MAPPING.md` | 화면별 API 호출 매핑 |
| `BACKEND_AI_LOCAL_RUN_GUIDE.md` | 로컬 환경 실행 가이드 |
| `IMPLEMENTATION_STATUS.md` | 구현 진행 상황 기록 |

### AI API 학습 문서 (`ai-api/`)

| 문서 | 설명 |
|------|------|
| `ai-api/README.md` | AI 계층 구조 개요 |
| `ai-api/AI_API_OVERVIEW_LEARNING.md` | ai-api 전체 개요 |
| `ai-api/ANALYZE_DIARY_API_LEARNING.md` | 일기 감정 분석 API |
| `ai-api/RISK_SCORE_API_LEARNING.md` | 위험도 점수 API |
| `ai-api/GENERATE_REPLY_API_LEARNING.md` | AI 답변 생성 API |
| `ai-api/RAG_CONTEXT_API_LEARNING.md` | RAG 문맥 검색 API |

---

## 문서들의 목적

이 문서들은 아래를 목표로 한다.

1. 각 API가 **왜 필요한지** 이해한다
2. **어떤 화면에서** 호출되는지 이해한다
3. **어떤 파일들이** 순차적으로 실행되는지 이해한다
4. Controller / Service / Repository / DTO / Entity **역할을 구분**한다
5. AI 에이전트에게 API를 요청할 때 **학습 가능한 형태**로 요청한다

---

## 권장 읽는 순서

### 1단계: 기본 이해

```
1. AUTH_API_LEARNING.md
2. DIARY_API_LEARNING.md
3. CALENDAR_API_LEARNING.md
4. CHAT_API_LEARNING.md
5. REPORT_API_LEARNING.md
6. DB_TABLE_SPECIFICATION.md
```

### 2단계: AI 계층 이해

```
7. ai-api/README.md
8. ai-api/AI_API_OVERVIEW_LEARNING.md
9. ai-api/ANALYZE_DIARY_API_LEARNING.md
10. ai-api/RISK_SCORE_API_LEARNING.md
11. ai-api/GENERATE_REPLY_API_LEARNING.md
12. ai-api/RAG_CONTEXT_API_LEARNING.md
```

### 3단계: 참조

```
- SCREEN_TO_API_MAPPING.md (필요시)
- BACKEND_AI_LOCAL_RUN_GUIDE.md (로컬 실행 시)
- IMPLEMENTATION_STATUS.md (작업 전)
```

---

## 왜 이 순서인가

| 순서 | 이유 |
|------|------|
| Auth 먼저 | 사용자 식별이 모든 기능의 시작점 |
| Diary 다음 | 핵심 기록 기능, 가장 기본적인 CRUD |
| Calendar 다음 | 기록을 조회하는 기능, Diary에 의존 |
| Chat 다음 | AI 연계가 가장 복잡한 흐름 |
| Report 다음 | 기록과 감정을 기간 단위로 요약 |
| DB 마지막 | 앞의 도메인을 이해한 후 테이블 관계 파악 |
| ai-api 후반 | backend-api 흐름을 먼저 이해해야 AI 연동이 이해됨 |

---

## 아키텍처 원칙

### Web Frontend → backend-api만 호출

```
✅ Responsive Web → backend-api
❌ Responsive Web → ai-api
❌ Responsive Web → ai-api-fastapi
```

**이유:**
- backend-api가 유일한 public API 진입점
- 인증/인가는 backend-api에서만 처리
- AI 실패가 전체 서비스를 중단시키면 안 됨

### ai-api / ai-api-fastapi는 내부 계층

```
backend-api → ai-api → ai-api-fastapi
```

| 서버 | 역할 | 호출 주체 |
|------|------|----------|
| backend-api | 공개 API, 비즈니스 로직 | 웹 클라이언트 |
| ai-api | AI 오케스트레이션 | backend-api (내부) |
| ai-api-fastapi | 감정분류 모델 서빙 | ai-api (내부) |

Frontend 개발자는 backend-api 문서만 보면 된다.
AI 내부 구조는 backend-api 개발자와 AI 개발자만 알면 된다.

---

## 새 도메인 문서 작성 가이드

새 도메인(예: Notification, Setting)을 추가할 때 아래 형식을 따른다.

### 파일명

```
{DOMAIN}_API_LEARNING.md
```

예: `NOTIFICATION_API_LEARNING.md`

### 문서 구조

```markdown
# {Domain} API 학습 문서

## 1. Goal
이 문서의 목적 3가지

## 2. Design Decision
왜 이 API가 필요한지, 어떤 선택을 했는지

## 3. Related Files
관련 파일 목록 (Controller, Service, Repository, DTO, Entity)

## 4. API Endpoints
각 엔드포인트별 설명

### 4-1. {엔드포인트명}

#### 왜 필요한가
#### 어느 화면에서 쓰는가
#### Request / Response
#### 실행 흐름
#### 예외 케이스
#### DB 영향

## 5. 조심해야 할 점
개발 시 주의사항

## 6. Next Step
다음에 해야 할 작업
```

### 체크리스트

새 문서 작성 후 확인:

- [ ] 이 README의 문서 목록에 추가했는가?
- [ ] 권장 읽는 순서에서 위치를 정했는가?
- [ ] `SCREEN_TO_API_MAPPING.md`에 화면 매핑을 추가했는가?
- [ ] `DB_TABLE_SPECIFICATION.md`에 테이블 명세가 있는가?

---

## AI API 문서 작성 가이드

ai-api 내부 엔드포인트 문서는 `ai-api/` 폴더에 작성한다.

### 파일명

```
{FEATURE}_API_LEARNING.md
```

예: `SUMMARIZE_WEEKLY_API_LEARNING.md`

### 문서 구조

Backend API 문서와 동일하되, 아래 섹션 추가:

```markdown
## AI 특화 섹션

### 프롬프트 전략
사용하는 프롬프트 템플릿과 이유

### 모델 선택
어떤 LLM/모델을 쓰는지, 왜 그 모델인지

### Fallback 전략
AI 실패 시 대체 동작

### Safety 고려사항
위험 감지, 안전 분기 처리
```

---

## 빠른 참조

### 화면별 어떤 문서를 봐야 하는가

| 화면 | 참조 문서 |
|------|----------|
| 로그인/회원가입 | `AUTH_API_LEARNING.md` |
| 일기 작성 | `DIARY_API_LEARNING.md` |
| 캘린더 | `CALENDAR_API_LEARNING.md` |
| 채팅 | `CHAT_API_LEARNING.md`, `ai-api/GENERATE_REPLY_API_LEARNING.md` |
| 리포트 | `REPORT_API_LEARNING.md` |

### 역할별 어떤 문서를 봐야 하는가

| 역할 | 필수 문서 |
|------|----------|
| Frontend 개발자 | Backend API 학습 문서, `SCREEN_TO_API_MAPPING.md` |
| Backend 개발자 | 전체 |
| AI 개발자 | `ai-api/` 전체, Backend API 학습 문서 |
