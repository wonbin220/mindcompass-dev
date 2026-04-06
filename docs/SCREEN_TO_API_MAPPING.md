# 화면별 API 매핑

이 문서는 반응형 웹이 `backend-api`만 호출하도록 화면별 API 연결 순서를 정리한 문서다.

전제:
- 반응형 웹은 `backend-api`만 호출한다.
- `ai-api`와 `ai-api-fastapi`는 Spring Boot 내부 호출 전용이다.

---

# 1. 로그인 / 회원가입

## 로그인 화면
- `POST /api/v1/auth/login`

사용 값:
- `accessToken`
- `refreshToken`
- `user.userId`
- `user.nickname`

## 회원가입 화면
- `POST /api/v1/auth/signup`

---

# 2. 홈 / 내 정보

## 내 정보 화면
- `GET /api/v1/users/me`

사용 값:
- `email`
- `nickname`
- `settings.notificationEnabled`
- `settings.responseMode`

---

# 3. Diary 화면

## 일기 작성 화면
- `POST /api/v1/diaries`

핵심 응답:
- `diaryId`
- `emotionTags`
- `riskLevel`
- `riskScore`
- `recommendedAction`

## 일기 상세 화면
- `GET /api/v1/diaries/{diaryId}`

핵심 응답:
- `title`
- `content`
- `primaryEmotion`
- `emotionTags`
- `riskLevel`
- `riskScore`
- `riskSignals`
- `recommendedAction`

## 날짜별 일기 목록
- `GET /api/v1/diaries?date=YYYY-MM-DD`

---

# 4. Calendar 화면

## 월간 캘린더 화면
- `GET /api/v1/calendar/monthly-emotions?year=YYYY&month=MM`

## 특정 날짜 요약
- `GET /api/v1/calendar/daily-summary?date=YYYY-MM-DD`

빈 날짜 정책:
- `200`
- `hasDiary=false`
- `diaryCount=0`

---

# 5. Chat 화면

## 채팅 세션 생성
- `POST /api/v1/chat/sessions`

## 세션 목록
- `GET /api/v1/chat/sessions`

## 세션 상세
- `GET /api/v1/chat/sessions/{sessionId}`

## 메시지 전송
- `POST /api/v1/chat/sessions/{sessionId}/messages`

핵심 응답:
- `assistantReply`
- `responseType`

`responseType` 해석:
- `NORMAL`: 일반 응답
- `SUPPORTIVE`: 중위험 지원형 응답
- `SAFETY`: 고위험 안전 안내 응답
- `FALLBACK`: ai-api 실패 fallback

---

# 6. Report 화면

## 월간 요약 카드
- `GET /api/v1/reports/monthly-summary?year=YYYY&month=MM`

## 최근 7일 감정 그래프
- `GET /api/v1/reports/emotions/weekly`

## 월간 위험도 그래프
- `GET /api/v1/reports/risks/monthly?year=YYYY&month=MM`

---

# 7. 웹 연동 시 주의 사항

- 변수명은 `sessionId`, `diaryId`처럼 정확히 맞춘다.
- Chat 경로는 반드시 복수형 `/sessions`를 사용한다.
- Diary enum은 서버 enum 값과 정확히 일치해야 한다.
- 잘못된 `year/month`는 `400`이다.
- AI 실패가 나도 Diary/Chat 저장은 유지될 수 있다.
- 브라우저 배포 환경에서는 CORS와 쿠키/토큰 저장 전략을 함께 정한다

---

# 8. 실제 UI 코드 수정이 가능한 조건

실제 화면 버튼, API client, 상태 관리, DTO 파싱 코드를 수정하려면 웹 앱 코드가 필요하다.

즉:
- 웹 앱 코드가 없으면 UI 코드는 못 바꾼다.
- 앱 코드가 없어도 API 계약/연동 순서/응답 해석 문서는 준비할 수 있다.
- 웹 앱 코드가 제공되면 그때부터 실제 화면 연동 작업을 바로 진행할 수 있다.
