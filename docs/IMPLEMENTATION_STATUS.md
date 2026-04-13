# Mind Compass 구현 진행 상황

이 문서는 프로젝트 구현 상태를 추적한다.
각 세션이 끝날 때 업데이트한다.

---

## Phase 진행 상황 요약

| Phase | 이름 | 구현 | 테스트 | 학습/검토 | 비고 |
  |-------|------|------|--------|-----------|------|
| 1 | Foundation | ✅ 완료 | - | ✅ | 골격 구조, 공통 모듈, 설정, DB 마이그레이션 |
| 2 | Auth / User | ✅ 완료 | ✅ 완료 | ⬜ 학습 필요 | Auth + User 테스트 모두 완료 |
| 3 | Diary CRUD | ✅ 완료 | ✅ 완료 | ✅ 완료 | 9+6=15 테스트, AI fallback 포함 |
| 4 | Calendar / Emotion | ✅ 완료 | ✅ 완료 | ✅ 완료 | 6+3=9 테스트, 월간 캘린더, 감정별 필터 |
| 5 | AI 연동 (ai-api + fastapi) | ✅ 완료 | ✅ 완료 | ⬜ 학습 필요 | 3개 서버 연동 구조, ai-api 4개 + fastapi SafetyNet |
| 6 | Chat | ✅ 완료 | ✅ 완료 | ⬜ 학습 필요 | Safety-first 패턴, 5+5=10 테스트 |
| 7 | Safety Net | ✅ 완료 | ✅ 완료 | ⬜ 학습 필요 | 키워드 분석 + AI 병합 |
| 8 | Reports | ✅ 완료 | ✅ 완료 | ⬜ 학습 필요 | 주간/월간 리포트 + 전월 비교, Controller 테스트 완료 |

> **현재 상태**: 모든 Phase의 코드 구현은 완료됨.
> 앞으로 할 일은 **Phase별 학습 → 테스트 작성 → 코드 검토** 순서로 진행.

---

## 학습 진행 가이드

### 추천 학습 순서

아래 순서대로 **한 Phase씩** 학습한다.
각 Phase에서 할 일: (1) 학습 문서 읽기 → (2) 코드 읽기 → (3) 로컬에서 API 호출 테스트 → (4) 단위 테스트 작성

```
Phase 2: Auth/User  ← 학습 문서 읽기 권장
Phase 3: Diary CRUD
Phase 4: Calendar
Phase 6: Chat (Phase 5보다 먼저 - backend-api 관점)
Phase 8: Reports
Phase 5: AI 연동 (ai-api 내부 구조 학습)
Phase 7: Safety Net (ai-api 내부 구조 학습)
```

### Phase별 학습 시 볼 파일

각 Phase에서 읽어야 할 파일을 정리했다.
**Controller → Service → Repository → DTO → Entity** 순서로 읽으면 흐름이 보인다.

---

## Phase 2: Auth / User (구현 완료)

### 학습 문서
- `docs/AUTH_API_LEARNING.md`

### 관련 파일 (읽는 순서)

| 순서 | 파일 | 역할 |
|------|------|------|
| 1 | `auth/controller/AuthController.java` | 3개 엔드포인트 (signup, login, refresh) |
| 2 | `auth/service/AuthService.java` | 비즈니스 로직 (비밀번호 인코딩, JWT 발급) |
| 3 | `auth/dto/request/SignupRequest.java` | 회원가입 요청 DTO |
| 4 | `auth/dto/request/LoginRequest.java` | 로그인 요청 DTO |
| 5 | `auth/dto/response/TokenResponse.java` | JWT 토큰 응답 DTO |
| 6 | `user/controller/UserController.java` | 3개 엔드포인트 (me, update, delete) |
| 7 | `user/service/UserService.java` | 프로필 조회/수정/탈퇴 |
| 8 | `user/domain/User.java` | 사용자 Entity |
| 9 | `common/security/SecurityConfig.java` | Spring Security 설정 |
| 10 | `common/security/JwtTokenProvider.java` | JWT 생성/검증 |

### 학습 포인트
- JWT 인증 흐름이 어떻게 동작하는가
- `@Transactional` 이 어디에 붙어있고 왜 그런가
- SecurityConfig에서 어떤 경로가 permitAll인가

### 테스트 상태
- [x] `AuthServiceTest.java` — 서비스 단위 테스트
- [x] `AuthControllerTest.java` — 컨트롤러 단위 테스트
- [x] `UserServiceTest.java` — 6개 테스트 완료
- [x] `UserControllerTest.java` — 3개 테스트 완료

---

## Phase 3: Diary CRUD (구현 완료)

### 학습 문서
- `docs/DIARY_API_LEARNING.md`

### 관련 파일 (읽는 순서)

| 순서 | 파일 | 역할 |
|------|------|------|
| 1 | `diary/controller/DiaryController.java` | 6개 엔드포인트 (CRUD + 재분석) |
| 2 | `diary/service/DiaryService.java` | 일기 비즈니스 로직 + AI 분석 연동 |
| 3 | `diary/dto/request/CreateDiaryRequest.java` | 생성 요청 DTO |
| 4 | `diary/dto/response/DiaryResponse.java` | 응답 DTO |
| 5 | `diary/domain/Diary.java` | 일기 Entity |
| 6 | `diary/repository/DiaryRepository.java` | JPA Repository |
| 7 | `infra/ai/AiDiaryAnalysisClient.java` | AI 분석 클라이언트 (fallback 패턴) |

### 학습 포인트
- **AI 실패가 일기 저장을 막지 않는 패턴** (`tryAnalyzeDiary` 메서드)
- 소유권 검증 (`validateOwnership`)
- `Optional<DiaryAnalysisResponse>` — AI 실패 시 empty 반환
- `ai.api.enabled=false` 설정으로 AI 호출 자체를 건너뛰는 방법

### 테스트 상태
- [x] `DiaryServiceTest.java` — 9개 테스트 (CRUD + AI fallback safety 패턴)
- [x] `DiaryControllerTest.java` — 6개 테스트 (6개 엔드포인트)

---

## Phase 4: Calendar / Emotion (구현 완료)

### 학습 문서
- `docs/CALENDAR_API_LEARNING.md`

### 관련 파일 (읽는 순서)

| 순서 | 파일 | 역할 |
|------|------|------|
| 1 | `calendar/controller/CalendarController.java` | 3개 엔드포인트 |
| 2 | `calendar/service/CalendarService.java` | 월간 캘린더, 날짜별/감정별 조회 |
| 3 | `calendar/dto/response/CalendarMonthResponse.java` | 월간 응답 DTO |
| 4 | `calendar/dto/response/CalendarDayResponse.java` | 일별 응답 DTO |

### 학습 포인트
- `YearMonth`, `LocalDate` 활용
- Stream API로 날짜별 매핑/감정 통계 계산
- Diary 도메인에 의존하는 구조

### 테스트 상태
- [x] `CalendarServiceTest.java` — 6개 테스트 (월별/날짜별/감정별 조회)
- [x] `CalendarControllerTest.java` — 3개 테스트 (3개 엔드포인트)

---

## Session Handoff - 2026-04-08 16:49

### 완료
- CalendarServiceTest 작성
- 월간 캘린더 조회, 날짜별 조회, 감정별 조회 테스트 검증

### 진행중
- CalendarControllerTest 작성 필요

### 다음 작업
- CalendarControllerTest에서 월간 조회, 특정 날짜 조회, 감정별 조회 응답 래핑 검증

### 블로커
- 없음

### 변경된 파일
- backend-api/src/test/java/com/mindcompass/api/calendar/service/CalendarServiceTest.java (신규)
- docs/IMPLEMENTATION_STATUS.md (수정)

### 주의사항
- CalendarService는 `totalDiaries`를 `days.hasDiary=true` 개수로 계산하므로, 테스트도 이 기준으로 검증했다.

---

## Phase 5: AI 연동 (구현 완료 — 별도 학습 권장)

> **주의**: 이 Phase는 ai-api와 ai-api-fastapi의 내부 구조다.
> backend-api를 먼저 이해한 후에 학습하는 것을 권장한다.
> **코드 수정 없이 학습/검토만 진행한다.**

### 학습 문서
- `docs/ai-api/README.md`
- `docs/ai-api/AI_API_OVERVIEW_LEARNING.md`
- `docs/ai-api/ANALYZE_DIARY_API_LEARNING.md`
- `docs/ai-api/RISK_SCORE_API_LEARNING.md`
- `docs/ai-api/GENERATE_REPLY_API_LEARNING.md`

### ai-api 관련 파일

| 파일 | 역할 |
|------|------|
| `controller/InternalAiController.java` | 내부 API 3개 엔드포인트 |
| `service/DiaryAnalysisService.java` | AI 일기 분석 + dev fallback |
| `service/RiskScoreService.java` | 키워드 분석(1차) + AI 분석(2차) 병합 |
| `service/ChatReplyService.java` | Safety-first 응답 생성 |
| `prompt/OpenAiPromptClient.java` | Spring AI ChatClient 호출 + JSON 파싱 |
| `prompt/KeywordRiskAnalyzer.java` | 키워드 기반 위험도 분석 |
| `prompt/PromptTemplates.java` | 프롬프트 템플릿 상수 |

### ai-api-fastapi 관련 파일

| 파일 | 역할 |
|------|------|
| `app/routers/model.py` | 감정분류 2개 엔드포인트 |
| `app/services/emotion_service.py` | 감정분류 서비스 + fallback |
| `app/inference/stub_predictor.py` | 키워드/해시 기반 stub 예측기 |
| `app/inference/base_predictor.py` | 예측기 추상 클래스 |
| `app/schemas/emotion.py` | 요청/응답 스키마 (Pydantic) |

### 학습 포인트
- 왜 서버를 3개로 나누는가 (관심사 분리)
- `backend-api → ai-api → ai-api-fastapi` 호출 흐름
- Zero-cost dev 원칙 (OpenAI 없이 개발)
- Safety-first: 키워드 분석이 AI보다 먼저 실행되는 이유
- StubPredictor 패턴: 실제 모델 없이 개발하는 방법

---

## Phase 6: Chat (구현 완료)

### 학습 문서
- `docs/CHAT_API_LEARNING.md`

### 관련 파일 (읽는 순서)

| 순서 | 파일 | 역할 |
|------|------|------|
| 1 | `chat/controller/ChatController.java` | 5개 엔드포인트 |
| 2 | `chat/service/ChatService.java` | 세션 관리, 메시지 + AI 응답 |
| 3 | `chat/domain/ChatSession.java` | 세션 Entity |
| 4 | `chat/domain/ChatMessage.java` | 메시지 Entity |
| 5 | `infra/ai/AiSafetyClient.java` | 안전 확인 클라이언트 (키워드 fallback) |
| 6 | `infra/ai/AiChatClient.java` | 채팅 AI 클라이언트 (fallback) |

### 학습 포인트
- **Safety-first 패턴**: 메시지 저장 → 위기 확인 → AI 응답 순서
- 위기 감지 실패 시 키워드 기반 fallback
- AI 실패 시 `DEFAULT_FALLBACK_MESSAGE` 반환
- 대화 히스토리를 AI에 전달하는 방법

### 테스트 상태
- [x] `ChatServiceTest.java` — 5개 서비스 단위 테스트 작성 완료
- [x] `ChatControllerTest.java` — 5개 MockMvc 테스트 작성 완료

---

## Session Handoff - 2026-04-08 17:03

### 완료
- ChatControllerTest 작성
- 세션 생성, 목록 조회, 메시지 목록 조회, 메시지 전송, 세션 종료 MockMvc 테스트 추가

### 진행중
- 없음

### 다음 작업
- Phase 6 Chat 테스트 코드 리뷰 및 필요 시 추가 예외 케이스 보강

### 블로커
- 없음

### 변경된 파일
- backend-api/src/test/java/com/mindcompass/api/chat/controller/ChatControllerTest.java (신규)
- docs/IMPLEMENTATION_STATUS.md (수정)

### 주의사항
- ChatController 테스트는 기존 컨트롤러 테스트 패턴과 동일하게 `@AutoConfigureMockMvc(addFilters = false)`를 사용해 `@CurrentUser`를 `null`로 주입받는 구조다.

---

## Session Handoff - 2026-04-08 17:06

### 완료
- ChatServiceTest 작성

---

## Session Handoff - 2026-04-08 17:24

### 완료
- ReportServiceTest 작성
- 주간 리포트의 감정 분포, 일별 추이, 일기 수, 채팅 수 테스트 추가
- 월간 리포트의 주별 요약, 전월 비교, 감정 분포 테스트 추가

### 진행중
- 없음

### 다음 작업
- ReportControllerTest에서 주간/월간 리포트 응답 래핑과 기본 파라미터 처리 검증

### 블로커
- 없음

### 변경된 파일
- backend-api/src/test/java/com/mindcompass/api/report/service/ReportServiceTest.java (신규)
- docs/IMPLEMENTATION_STATUS.md (수정)

### 주의사항
- ReportService의 주차 계산은 달력 주가 아니라 `1~7일, 8~14일` 고정 구간이므로 테스트도 이 기준으로 검증했다.

---

## Session Handoff - 2026-04-09 15:45

### 완료
- `dataset_sample_80_input.csv` 기준 한국어 번역본 `dataset_sample_80_input_ko.csv` 생성
- `Depression_Text_processed.csv`의 `text_ko` 공란 전체 채움

### 진행중
- 없음

### 다음 작업
- 다른 영어 원문 CSV가 추가되면 같은 방식으로 `_ko.csv` 파생본 생성

### 블로커
- 없음

### 변경된 파일
- csv파일들/human-and-llm-mental-health-conversations/dataset_sample_80_input_ko.csv (신규)
- csv파일들/student-depression-text/Depression_Text_processed.csv (수정)
- docs/IMPLEMENTATION_STATUS.md (수정)

### 주의사항
- `dataset_sample_80_input_ko.csv`는 기존 `dataset_ko.csv`의 동일 행 번역을 재사용했으므로 원문 정합성이 유지된다.

---

## Session Handoff - 2026-04-12 (Opus)

### 완료
- **Phase 2 User 테스트 완료**: UserServiceTest(6개), UserControllerTest(3개) 작성 및 통과
- **Phase 8 Report Controller 테스트 완료**: ReportControllerTest(4개) 작성 및 통과
- **Apidog 컬렉션 생성**: Chat, Report, Calendar 3개 컬렉션 신규 생성
- **Apidog 실제 테스트 완료**: Auth, User, Diary, Calendar, Chat, Report 전체 API 동작 확인
- **학습 문서 신규 작성**: `docs/USER_API_LEARNING.md`
- **학습 문서 업데이트**: `docs/REPORT_API_LEARNING.md`에 ReportControllerTest 가이드 추가
- **CLAUDE.md에 김영한 강사 페르소나 추가**: 학습-first 모드 설정

### backend-api 테스트 현황 (전체)

| Phase | Service Test | Controller Test | Apidog |
|-------|-------------|-----------------|--------|
| 2 Auth | ✅ | ✅ | ✅ |
| 2 User | ✅ | ✅ | ✅ |
| 3 Diary | ✅ | ✅ | ✅ |
| 4 Calendar | ✅ | ✅ | ✅ |
| 6 Chat | ✅ | ✅ | ✅ (fallback 정상) |
| 8 Report | ✅ | ✅ | ✅ (빈 감정 정상) |

> **backend-api 단독 테스트는 사실상 완료.**

### 진행중
- 없음

### 다음 작업
- **Phase 5: AI 연동 학습** (ai-api, ai-api-fastapi 내부 구조 학습)
- Phase 5, 7 테스트 작성 (Codex 위임 예정)
- Sonnet 세션으로 진행 권장 (토큰 절약)

### 블로커
- 없음

### 변경된 파일
- CLAUDE.md (수정 — 김영한 강사 페르소나 추가)
- docs/USER_API_LEARNING.md (신규)
- docs/REPORT_API_LEARNING.md (수정 — 섹션 8 ReportControllerTest 가이드 추가)
- docs/apidog/MindCompass_Chat_API.postman_collection.json (신규)
- docs/apidog/MindCompass_Report_API.postman_collection.json (신규)
- docs/apidog/MindCompass_Calendar_API.postman_collection.json (신규)
- docs/IMPLEMENTATION_STATUS.md (수정)

### 주의사항
- Calendar 특정 날짜 조회(GET /calendar/{date})에서 같은 날짜에 일기 2개 이상이면 C002 에러 발생. `findByUserIdAndDiaryDate()`가 Optional 반환이라 중복 시 에러. 서버 재시작(H2 초기화) 후 재테스트하면 해결.
- Chat Apidog 테스트에서 AI 응답은 fallback("죄송합니다...") — ai-api 미실행 상태에서 정상 동작.
- Report Apidog 테스트에서 감정 분포는 빈 상태 — AI 미분석 일기라 primaryEmotion=null이므로 정상.
- UserServiceTest, UserControllerTest, ReportControllerTest는 학습자가 학습 문서를 보고 직접 따라 친 것.
