# Mind Compass 구현 진행 상황

이 문서는 프로젝트 구현 상태를 추적한다.
각 세션이 끝날 때 업데이트한다.

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