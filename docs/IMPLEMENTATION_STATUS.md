# Mind Compass 구현 진행 상황

이 문서는 프로젝트 구현 상태를 추적한다.
각 세션이 끝날 때 업데이트한다.

---
## 2026-05-08 — 프론트엔드 인증/UX 개선

### 완료

#### 인증 흐름 강화
- `api.ts`: 401 발생 시 refresh token으로 자동 갱신 시도, 실패 시 로그아웃
- `api.ts`: `clearTokens()` 추가 — access_token + refresh_token 동시 삭제
- `login/page.tsx`: 로그인 성공 시 refresh_token도 localStorage에 저장
- `AppNav.tsx`: 로그아웃 시 refresh_token도 함께 삭제

#### 설정 페이지
- `app/(app)/settings/page.tsx` 신규 생성
- `GET /api/v1/users/me` 연동 — 닉네임, 이메일, 가입일 표시
- `PATCH /api/v1/users/me` 연동 — 닉네임 인라인 수정 (2~50자 검증)
- AppNav에 설정 메뉴(⚙️ ) 추가

#### 채팅 세션 목록
- `chat/page.tsx` 전면 개편 — 항상 새 세션 생성 → 세션 목록 화면 우선 표시
- `GET /api/v1/chat/sessions` 연동 (Spring Page 응답 → `content` 필드 추출)
- `GET /api/v1/chat/sessions/{id}` 연동 — 기존 세션 메시지 이어보기
- `POST /api/v1/chat/sessions/{id}/close` 연동 — 대화 종료 버튼
- "← 목록" 버튼으로 세션 목록 ↔ 채팅 화면 전환

#### UX 개선
- 일기 저장 후 라우팅: `/calendar` → `/diary?date=YYYY-MM-DD`

### 변경된 파일
- `web-app/src/lib/api.ts`
- `web-app/app/login/page.tsx`
- `web-app/src/components/AppNav.tsx`
- `web-app/app/(app)/settings/page.tsx` (신규)
- `web-app/app/(app)/chat/page.tsx`
- `web-app/app/(app)/diary/new/page.tsx`

### API 응답 구조 확인 사항
- `GET /api/v1/chat/sessions` → Spring Page 객체: `{ content: ChatSession[], totalPages, ... }`
- `GET /api/v1/chat/sessions/{id}` → 메시지 배열 직접 반환: `Message[]`

#### 보안 강화
- `login/page.tsx`: `from` 파라미터 open redirect 방어 — `startsWith("/") && !startsWith("//")` 검증
- `login/page.tsx`: 로그인 성공 시 `refresh_token` cookie에도 병행 저장 (max-age=604800)

#### 입력값 검증
- `diary/new/page.tsx`: 제목/내용 공백만 입력 시 차단 (`trim()` 검증)
- `diary/new/page.tsx`: `React.FormEvent` → `React.FormEvent<HTMLFormElement>` 타입 수정

#### 채팅 세션 종료
- `chat/page.tsx`: "대화 종료" 버튼 추가 — `POST /api/v1/chat/sessions/{id}/close` 연동
- 종료 후 세션 목록 화면으로 복귀, 목록 내 해당 세션 상태 "CLOSED"로 즉시 반영

### 변경된 파일 (추가)
- `web-app/app/login/page.tsx`
- `web-app/app/(app)/diary/new/page.tsx`
- `web-app/app/(app)/chat/page.tsx`

#### 일기 수정/삭제 페이지 확인 및 개선
- `diary/[id]/page.tsx`: 삭제 후 `/diary?date=${diary?.writtenAt.slice(0,10)}` 라우팅 개선
- `diary/[id]/edit/page.tsx`: trim() 검증, `React.FormEvent<HTMLFormElement>` 타입 수정
- 연결 흐름 정상 확인: `/diary?date=` → `/diary/{id}` → `/diary/{id}/edit`

### 다음 작업
- (완료됨 — 아래 2026-05-08 2차 세션 참고)

## 2026-05-08 — HttpOnly 쿠키 전환 + 회원 탈퇴

### 완료

#### 보안: HttpOnly 쿠키 전환 (localStorage → Set-Cookie)
- `AuthController.java`: 로그인/리프레시 응답을 JSON body 대신 HttpOnly 쿠키로 전환
- `AuthController.java`: `POST /api/v1/auth/logout` 추가 — 쿠키 max-age=0 삭제
- `JwtAuthenticationFilter.java`: 쿠키 우선 읽기, Authorization 헤더 fallback
- `next.config.ts`: `/api/**` → `http://localhost:8080` proxy rewrite 추가
- `api.ts`: localStorage 토큰 코드 전면 제거, `credentials: "include"` 추가, 상대경로 전환
- `login/page.tsx`: localStorage/document.cookie 저장 코드 제거
- `AppNav.tsx`: `POST /api/v1/auth/logout` 호출로 로그아웃 처리

#### 설정 페이지: 회원 탈퇴
- `settings/page.tsx`: 회원 탈퇴 UI 추가 — 2단계 확인 후 `DELETE /api/v1/users/me` 호출
- 탈퇴 성공 시 logout API 호출 후 `/login` 리다이렉트

### 변경된 파일
- `backend-api/.../AuthController.java`
- `backend-api/.../JwtAuthenticationFilter.java`
- `web-app/next.config.ts`
- `web-app/src/lib/api.ts`
- `web-app/app/login/page.tsx`
- `web-app/src/components/AppNav.tsx`
- `web-app/app/(app)/settings/page.tsx`

  ---
## 2026-05-08 — 미들웨어 + 일기 감정 수정 + AI 체인 연동

### 완료

#### 미들웨어: 라우트 보호
- `middleware.ts`: 미인증 접근 시 `/login?from=pathname` 리다이렉트
- 보호 경로: `/calendar`, `/diary`, `/chat`, `/report`, `/settings`

#### 일기 수정: 감정/강도 편집
- `diary/[id]/edit/page.tsx`: 감정 선택 + 강도(1~5) UI 추가
- `UpdateDiaryRequest.java`: `primaryEmotion`, `emotionIntensity` 필드 추가
- `DiaryService.updateDiary()`: 감정 업데이트 + 감정 태그 동기화

#### AI 체인 연동: backend-api → ai-api → ai-api-fastapi
- `FastApiEmotionClient.java` 신규: ai-api에서 FastAPI 감정분류 HTTP 호출
- `ai-api/application.yml`: `fastapi.emotion.base-url` 공통 설정 추가
- `DiaryAnalysisService.java`: FastAPI 우선 → OpenAI 요약 보강 → keyword fallback 흐름
- `DiaryAnalysisService.java`: 한국어 감정 레이블 매핑 추가 (EMOTION_KO)
- AI 분석 표시: "오늘 일기에서 '기쁨' 감정이 감지되었습니다. (신뢰도 99%)" 형태로 개선
- 3서버 전체 체인 동작 확인 완료 (emotion=happy, confidence=0.999)

### 변경된 파일
- `web-app/middleware.ts`
- `web-app/app/(app)/diary/[id]/edit/page.tsx`
- `backend-api/.../UpdateDiaryRequest.java`
- `backend-api/.../DiaryService.java`
- `ai-api/.../client/FastApiEmotionClient.java` (신규)
- `ai-api/.../service/DiaryAnalysisService.java`
- `ai-api/src/main/resources/application.yml`

## 2026-05-08 — 리포트/채팅/위험도 UI 개선

### 완료

#### 리포트 페이지 개선
- `report/page.tsx`: `totalChats` 필드 타입 추가 및 요약 카드에 채팅 횟수 표시
- `report/page.tsx`: `EmotionComparison` 타입 추가, 월간 전월 대비 카드 구현 (일기 수/감정 강도/추세)

#### 채팅 UX 개선
- `chat/page.tsx`: `useRef` + `useEffect`로 새 메시지 도착 시 자동 스크롤
- `chat/page.tsx`: SUPPORTIVE 응답 스타일 추가 (amber 계열)
- `chat/page.tsx`: `input` → `textarea` 교체, Shift+Enter 줄바꿈 지원

#### 일기 위험도 표시
- `ai-api/.../AnalyzeDiaryResponse.java`: `riskLevel` 필드 추가
- `ai-api/.../DiaryAnalysisService.java`: `RiskScoreService` 주입, 키워드 기반 위험도 분석 항상 수행
- `diary/[id]/page.tsx`: 위험도 배지에 툴팁 추가 (hover/touch 시 낮음/보통/높음 설명)

### 변경된 파일
- `web-app/app/(app)/report/page.tsx`
- `web-app/app/(app)/chat/page.tsx`
- `web-app/app/(app)/diary/[id]/page.tsx`
- `ai-api/.../dto/response/AnalyzeDiaryResponse.java`
- `ai-api/.../service/DiaryAnalysisService.java`

## 2026-05-07 — 하루 최대 3개 일기 + 날짜별 목록 페이지

### 완료
- 백엔드: `DiaryService.createDiary()` 에 일일 제한(3개) 검증 추가
- 백엔드: `ErrorCode.DIARY_DAILY_LIMIT_EXCEEDED (D003)` 추가
- 백엔드: `CalendarDayResponse` → `diaries: List<DiaryBriefInfo>` 로 변경 (단일 → 다중 지원)
- 백엔드: `CalendarService.getDiaryByDate()` → `List<DiaryListResponse>` 반환으로 변경
- 프론트: `/diary?date=YYYY-MM-DD` 날짜별 일기 목록 페이지 신규 생성
- 프론트: 캘린더 날짜 클릭 → 목록 페이지로 이동, 이모지 최대 3개 표시
- 프론트: 일기 작성 헤더에 "하루 최대 3개" 문구 추가

### 변경된 파일
- `backend-api/.../ErrorCode.java`
- `backend-api/.../DiaryRepository.java`
- `backend-api/.../DiaryService.java`
- `backend-api/.../CalendarDayResponse.java`
- `backend-api/.../CalendarService.java`
- `backend-api/.../CalendarController.java`
- `web-app/app/(app)/diary/page.tsx` (신규)
- `web-app/app/(app)/diary/new/page.tsx`
- `web-app/app/(app)/calendar/page.tsx`

### UX 설계 결정
- 3개 도달 시 에러 팝업이 아닌 "새로 쓰기" 버튼 자체를 숨기는 방식으로 처리
- "오늘의 기록이 가득 찼어요 ✨" 메시지로 긍정적 피드백 제공
- 백엔드 에러(D003)는 직접 API 호출 우회 경로를 위한 안전망으로만 동작

---

## Phase 진행 상황 요약

| Phase | 이름 | 구현 | 테스트 | 학습/검토 | 비고 |
  |-------|------|------|--------|-----------|------|
| 1 | Foundation | ✅ 완료 | - | ✅ | 골격 구조, 공통 모듈, 설정, DB 마이그레이션 |
| 2 | Auth / User | ✅ 완료 | ✅ 완료 | ✅ 완료 | Auth + User 테스트 모두 완료 |
| 3 | Diary CRUD | ✅ 완료 | ✅ 완료 | ✅ 완료 | 9+6=15 테스트, AI fallback 포함 |
| 4 | Calendar / Emotion | ✅ 완료 | ✅ 완료 | ✅ 완료 | 6+3=9 테스트, 월간 캘린더, 감정별 필터 |
| 5 | AI 연동 (ai-api + fastapi) | ✅ 완료 | ✅ 완료 | ✅ 완료 | 3개 서버 연동 구조, ai-api 4개 + fastapi SafetyNet |
| 6 | Chat | ✅ 완료 | ✅ 완료 | ✅ 완료 | Safety-first 패턴, 5+5=10 테스트 |
| 7 | Safety Net | ✅ 완료 | ✅ 완료 | ✅ 완료 | 키워드 분석 + AI 병합 |
| 8 | Reports | ✅ 완료 | ✅ 완료 | ✅ 완료 | 주간/월간 리포트 + 전월 비교, Controller 테스트 완료 |

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

---

## Session Handoff - 2026-04-22 15:59

### 완료
- ANGRY, SAD, ANXIOUS 경계 문장 각 10개씩 한국어 CSV 산출물 작성
- 감정 분류 학습용 `processed/` 형식에 맞춰 `text,label` 헤더로 정리

### 진행중
- 없음

### 다음 작업
- 필요하면 이 CSV를 기존 학습 데이터와 병합하거나 수동 품질 검토 기준을 추가

### 블로커
- 없음

### 변경된 파일
- ai-api-fastapi/training/emotion_classifier/processed/korean_emotion_boundary_sentences.csv (신규)
- docs/IMPLEMENTATION_STATUS.md (수정)

### 주의사항
- 이번 문장들은 경계 사례를 의도해 감정 강도를 중간 수준으로 맞췄다.
- 라벨은 단일 주감정 기준이며, 실제 학습 투입 전 중복/유사 표현 검토가 필요할 수 있다.

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

## Session Handoff - 2026-05-04 15:40

### 완료
- backend-api의 User, Diary, Chat 엔티티를 신규 ERD에 맞게 갱신
- DiaryAiAnalysis, DiaryEmotionTag, RefreshToken, UserSettings, SafetyEvent, AiAuditLog 엔티티 추가
- Diary/User/Auth 관련 DTO와 서비스 참조를 신규 필드명으로 정리
- Flyway 마이그레이션 `V004`~`V009` 추가
- 주요 테스트 코드에서 `nickname`, `writtenAt`, `emotionIntensity` 기준으로 참조명 갱신

### 진행중
- 없음

### 다음 작업
- 네트워크/인터페이스 제약이 없는 환경에서 `backend-api ./gradlew compileJava` 재실행
- 필요하면 이어서 `./gradlew test`로 테스트 소스 정합성 추가 검증

### 블로커
- 현재 Codex 샌드박스에서는 Gradle이 파일 락 리스너 초기화 중 네트워크 인터페이스 정보를 얻지 못해 `compileJava`가 시작 단계에서 실패함

### 변경된 파일
- backend-api/src/main/java/com/mindcompass/api/user/domain/User.java
- backend-api/src/main/java/com/mindcompass/api/diary/domain/* (신규/수정)
- backend-api/src/main/java/com/mindcompass/api/chat/domain/* (수정)
- backend-api/src/main/java/com/mindcompass/api/auth/domain/RefreshToken.java (신규)
- backend-api/src/main/java/com/mindcompass/api/user/domain/UserSettings.java (신규)
- backend-api/src/main/java/com/mindcompass/api/safety/domain/SafetyEvent.java (신규)
- backend-api/src/main/java/com/mindcompass/api/infra/ai/domain/AiAuditLog.java (신규)
- backend-api/src/main/resources/db/migration/V004__*.sql ~ V009__*.sql (신규)
- backend-api/src/test/java/com/mindcompass/api/... (관련 테스트 다수 수정)

### 주의사항
- Diary 삭제는 물리 삭제 대신 `deletedAt` 소프트 삭제로 전환했다.
- Diary AI 분석 결과는 Diary 본문 컬럼이 아니라 `diary_ai_analyses`와 `diary_emotion_tags`에 분리 저장되도록 맞췄다.
- ChatControllerTest 작성
- 채팅 세션 생성, 목록 조회, 세션 상세, 메시지 전송, 세션 종료 API 테스트 검증

### 진행중
- 없음

### 다음 작업
- Reports 도메인 학습 문서 또는 테스트 작업 진행

### 블로커
- 없음

### 변경된 파일
- backend-api/src/test/java/com/mindcompass/api/chat/controller/ChatControllerTest.java (신규)
- docs/IMPLEMENTATION_STATUS.md (수정)

### 주의사항
- 메시지 전송 테스트는 AI 실제 호출이 아니라 service mock 기반 계약 검증이다.

---

## Session Handoff - 2026-04-22 16:45

### 완료
- ai-api 내부 엔드포인트용 Postman 컬렉션 작성
- `analyze-diary`, `risk-score`, `generate-reply` 요청 예시와 테스트 스크립트 추가

### 진행중
- 없음

### 다음 작업
- 필요하면 Apidog/Postman import 후 로컬 `ai-api` 실행 상태에서 컬렉션 smoke test 수행

### 블로커
- 없음

### 변경된 파일
- docs/apidog/MindCompass_AI_API.postman_collection.json (신규)
- docs/IMPLEMENTATION_STATUS.md (수정)

### 주의사항
- `ai-api` 기본 로컬 환경에서는 OpenAI 비활성화로 fallback 응답이 내려올 수 있어 일부 테스트를 AI 성공과 dev fallback 모두 허용하도록 작성했다.

---

## Session Handoff - 2026-04-23 00:00

### 완료
- ai-api Spring Boot 버전 4.0.5 → 3.5.0 다운그레이드 (Spring AI 1.0.0 호환성 문제 해결)
- ai-api dev 프로필 `api-key` 빈 값 → `dev-placeholder` 로 수정 (bean 생성 시 검증 통과)
- InternalAiControllerTest import 경로 수정 (Boot 4.x → 3.x 패키지 경로)
- ai-api bootRun 정상 기동 확인 (port 8081, dev 프로필)
- ai-api 3개 엔드포인트 Apidog smoke test 완료 (200 OK, fallback 응답 구조 확인)
- ai-api-fastapi Postman 컬렉션 작성 (MindCompass_FastAPI_Emotion.postman_collection.json)
- ai-api-fastapi 앱 전용 venv 분리 필요 확인 (.venv-app, requirements.txt 기반)

### 진행중
- v5 감정분류 모델 학습 중 (nohup 백그라운드, /tmp/tired_v5_train.log)
- 마지막 확인 시 약 10% 진행 (1041/10380 step), 내일 새벽 5~6시 완료 예상

### 다음 작업
1. v5 학습 완료 후 best 모델 경로 확인 (artifacts/tired_v5/best/)
2. ai-api-fastapi StubPredictor → 실제 KcELECTRA 모델로 교체
3. ai-api-fastapi .venv-app 생성 후 서버 기동 테스트
4. Spring AI 2.0 GA 출시(2026-05-28 예정) 후 ai-api Spring Boot 4.0 + Spring AI 2.0으로 업그레이드
5. ai-api 학습 문서 읽기 (AI_API_OVERVIEW_LEARNING.md 부터)

### 블로커
- 없음

### 변경된 파일
- ai-api/build.gradle (Spring Boot 4.0.5 → 3.5.0, webmvc-test 의존성 제거)
- ai-api/src/main/resources/application.yml (exclusions 제거, dev api-key 수정)
- ai-api/src/test/java/com/mindcompass/ai/controller/InternalAiControllerTest.java (import 경로 수정)
- docs/apidog/MindCompass_FastAPI_Emotion.postman_collection.json (신규)
- docs/IMPLEMENTATION_STATUS.md (수정)

### 주의사항
- ai-api는 Spring Boot 3.5.0 + Spring AI 1.0.0 조합으로 동작 중
- Spring AI 2.0 GA(2026-05-28 예정) 이후 Boot 4.0으로 다시 올릴 수 있음
- ai-api-fastapi 서버 실행 시 training용 .venv가 아닌 .venv-app을 사용해야 함
- v5 학습은 nohup으로 실행 중이므로 터미널 종료해도 계속 돌아감

---

## Session Handoff - 2026-04-24

### 완료
- ai-api-fastapi StubPredictor → 실제 KcELECTRA 모델(tired_v5) 교체 완료
- tired_v5 단독 평가: TIRED만 정확(98.9%), 나머지 5개 감정 실패 확인
- HuggingFace 한국어 감정분류 모델 탐색 (Seonghaa, LimYeri)
- HybridPredictor 구현: tired_v5(TIRED 전담) + LimYeri(나머지 5개) 앙상블
- 최종 테스트: 6개 감정 전부 99%+ 정확도 확인

### 모델 구성 (현재)
- TIRED 전담: `training/emotion_classifier/artifacts/tired_v5/best`
- 나머지 5개 감정: `LimYeri/HowRU-KoELECTRA-Emotion-Classifier`
    - HuggingFace: https://huggingface.co/LimYeri/HowRU-KoELECTRA-Emotion-Classifier
    - 로컬 경로: `/tmp/limyeri_model` (서버 재시작 시 재다운로드 필요)
    - 선택 이유: 일기/상담 도메인 학습 데이터, 99%+ confidence

### 진행중
- 없음

### 다음 작업
1. LimYeri 모델을 `/tmp` 대신 프로젝트 내부 경로로 이동 (서버 재시작 대응)
2. Spring AI 2.0 GA(2026-05-28 예정) 후 ai-api Spring Boot 4.0 업그레이드

### 블로커
- LimYeri 모델이 `/tmp`에 있어서 서버 재시작 시 경로는 유지되나
  OS 재부팅 시 삭제될 수 있음 → 영구 경로 이동 필요

### 변경된 파일
- ai-api-fastapi/app/inference/kcelectra_predictor.py (신규)
- ai-api-fastapi/app/inference/hybrid_predictor.py (신규)
- ai-api-fastapi/app/services/emotion_service.py (HybridPredictor로 교체)
- ai-api-fastapi/requirements.txt (torch, transformers 추가)

### all_v1 실험 결과 (KcELECTRA 6클래스 단독 학습 시도)

AIHub 감성대화말뭉치 원본 JSON으로 KcELECTRA를 6클래스 전체 학습 시도함.

**결과: 채택하지 않음**

| 항목 | all_v1 | 하이브리드 (현재) |
  |--|--|--|
| 정확도 | 47% | 99% |
| TIRED F1 | 0.00 | 98.9% |
| macro F1 | 0.40 | - |

**실패 원인**
1. TIRED 학습 데이터 253개, 검증 3개 — 사실상 미학습
2. AIHub 감성대화 데이터가 일반 대화 도메인 → 일기/상담 도메인인 LimYeri보다 정확도 낮음

**결론**
- KcELECTRA 단독으로 6클래스를 잘 잡으려면 각 감정별 고품질 균등 데이터가 필요
- 현재 보유 데이터로는 불가능
- 현재 하이브리드 구조(tired_v5 + LimYeri)가 최선
- all_v1 모델은 폐기, artifacts/all_v1은 참고용으로만 보존

---

## Session Handoff - 2026-05-07

### 완료

#### 프론트엔드 — 캘린더 / 일기 / 리포트 페이지

- **캘린더 페이지** (`/calendar`): 날짜 클릭 시 `/diary?date=YYYY-MM-DD` 목록 페이지로 이동
- **캘린더 페이지**: `CalendarDay` 구조를 단일 diary → `diaries: DiaryBrief[]`로 변경, 이모지 최대 3개 표시
- **날짜별 일기 목록 페이지** (`/diary?date=...`) 신규 생성: 해당 날짜 일기 목록 + 3개 도달 시 "새로 쓰기" 버튼 숨김
- **일기 작성 페이지** (`/diary/new`): URL `?date=` 파라미터로 날짜 자동 입력, 헤더에 "하루 최대 3개" 문구 추가
- **일기 작성 페이지**: D003 에러 시 친절한 한국어 메시지로 표시 ("이 날은 이미 3개의 일기를 작성했어요 😊")
- **AppNav**: "Mind Compass" 로고 클릭 시 `/calendar`로 이동
- **리포트 페이지** (`/report`): 실제 API 연동(`/api/v1/reports/weekly`, `/api/v1/reports/monthly`)
- **리포트 페이지**: SVG 기반 SparklineChart 컴포넌트 추가 (주간 dailyTrends, 월간 weeklySummaries)
- **리포트 페이지**: 감정 분포 막대 그래프(EmotionBars) 구현

#### 백엔드 — 하루 3개 일기 제한

- `DiaryService.createDiary()`: 일일 3개 초과 시 `BusinessException(D003)` 발생
- `ErrorCode.DIARY_DAILY_LIMIT_EXCEEDED (D003)` 추가
- `CalendarDayResponse`: 단일 diary 필드 → `List<DiaryBriefInfo> diaries` 로 변경
- `CalendarService.getDiaryByDate()`: `List<DiaryListResponse>` 반환으로 변경
- `CalendarController.getDiaryByDate()`: 반환 타입 변경에 맞게 업데이트
- `DiaryRepository`: `countByUserIdAndWrittenAtBetween...` 메서드 추가

#### 학습 문서

- `docs/FRONTEND_PAGES_LEARNING.md` 신규 생성 (캘린더/일기/리포트 프론트엔드 구현 패턴)
- `docs/DIARY_API_LEARNING.md`, `docs/DB_TABLE_SPECIFICATION.md`, `docs/SCREEN_TO_API_MAPPING.md` 3개 일기 제한 반영

### 진행중
- 없음

### 다음 작업 (프론트엔드 — 우선순위 순)

1. **401 자동 로그아웃** (높음): `web-app/src/lib/api.ts`에서 401 감지 시 토큰 삭제 + `/login` 리다이렉트
2. **리프레시 토큰 자동 갱신** (높음): 401 발생 시 `POST /api/v1/auth/refresh` 먼저 시도, 실패하면 로그아웃
3. **프로필/설정 페이지** (중간): `GET /api/v1/users/me` 연동, 내 정보 표시
4. **채팅 이전 세션 목록** (중간): `GET /api/v1/chat/sessions` 연동, 이전 대화 이력 접근 가능
5. **일기 저장 후 라우팅** (낮음): 저장 후 `/diary?date=YYYY-MM-DD`로 이동 (현재는 `/calendar`)

### 블로커
- 없음

### 변경된 파일
- `web-app/app/(app)/calendar/page.tsx`
- `web-app/app/(app)/diary/page.tsx` (신규)
- `web-app/app/(app)/diary/new/page.tsx`
- `web-app/app/(app)/report/page.tsx`
- `web-app/src/components/AppNav.tsx`
- `backend-api/.../ErrorCode.java`
- `backend-api/.../DiaryRepository.java`
- `backend-api/.../DiaryService.java`
- `backend-api/.../CalendarDayResponse.java`
- `backend-api/.../CalendarService.java`
- `backend-api/.../CalendarController.java`
- `docs/FRONTEND_PAGES_LEARNING.md` (신규)
- `docs/DIARY_API_LEARNING.md`
- `docs/DB_TABLE_SPECIFICATION.md`
- `docs/SCREEN_TO_API_MAPPING.md`

### 주의사항
- 캘린더 `dayData.diaries` 필드는 optional chaining(`?.`)으로 방어 처리 필수 (`?.length ?? 0`)
- SparklineChart는 외부 라이브러리 없이 순수 SVG로 구현 (viewBox 기반 반응형)
- 3개 제한 UX는 에러 팝업이 아닌 버튼 숨김 방식 — 백엔드 D003은 직접 API 우회 방어용
