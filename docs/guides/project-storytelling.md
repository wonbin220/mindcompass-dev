# Project Storytelling Guide

이 문서는 Mind Compass의 README, 프로젝트 소개문, 포트폴리오 설명, 이력서 항목, 면접용 프로젝트 설명을 쓸 때 따르는 기준이다.

목표는 기술 나열이 아니라 문제 해결 중심으로 프로젝트를 설명하는 것이다.

## 1. 핵심 원칙

프로젝트 설명은 아래 순서를 우선한다.

1. 어떤 문제를 해결하려는 프로젝트인가
2. 왜 지금의 구조를 선택했는가
3. 어떤 사용자 흐름을 보호하려 했는가
4. 어떤 검증 포인트가 있는가
5. 아직 남은 한계와 다음 개선은 무엇인가

기술 스택은 필요하지만, 소개의 첫 줄이 되어서는 안 된다.

좋은 설명:
- 문제를 먼저 말한다
- 설계 의도를 설명한다
- trade-off를 숨기지 않는다
- fallback과 안전 분기를 구체적으로 설명한다
- 확인된 테스트와 현재 구현 사실만 사용한다

피해야 할 설명:
- 스택만 길게 나열하는 소개
- 없는 성능 수치나 운영 지표를 있는 것처럼 쓰는 문장
- 아직 구현되지 않은 기능을 구현된 것처럼 표현하는 문장
- current 구조와 legacy 구조를 섞어서 쓰는 문장

## 2. Mind Compass에서 먼저 보여줘야 하는 문제

이 프로젝트에서 가장 먼저 보여줘야 하는 문제는 아래와 같다.

- 멘탈 헬스 기록/상담 서비스에서는 AI가 실패해도 기록 저장은 유지되어야 한다.
- 위험 신호 판단은 일반 답변 생성보다 먼저 와야 한다.
- 공개 API와 내부 AI 계층이 강하게 결합되면 구조가 빠르게 복잡해진다.

즉, 이 프로젝트의 핵심 메시지는 "AI를 붙인 서비스"가 아니라  
"AI가 실패해도 핵심 사용자 흐름이 무너지지 않도록 설계한 서비스"여야 한다.

## 3. 이 저장소에서 강조해야 할 구조

현재 기준 구조:

`Responsive Web -> backend-api -> ai-api -> ai-api-fastapi`

각 계층 설명은 아래 관점을 유지한다.

### backend-api
- 공개 API 진입점
- 인증, 저장, 조회, 권한 검증
- diary/chat/calendar/report 비즈니스 흐름 중심

### ai-api
- 내부 AI 오케스트레이터
- analyze-diary, risk-score, generate-reply 조율
- safety, fallback, future RAG orchestration

### ai-api-fastapi
- 감정분류 모델 서빙 계층
- 모델 버전, threshold, calibration, runtime metadata 관리

중요:
- 클라이언트는 반드시 `backend-api`만 호출한다.
- `ai-api-fastapi`는 공개 API처럼 설명하지 않는다.
- ai-api는 단순 추론 서버가 아니라 orchestration layer로 설명한다.

## 4. README/포트폴리오 추천 구조

README나 프로젝트 소개문은 아래 순서를 추천한다.

1. 한 줄 문제 정의
2. 이 프로젝트가 해결하려는 핵심 문제
3. 왜 3계층 구조를 선택했는가
4. 대표 사용자 흐름 1개
5. 현재 구현 범위
6. 검증 포인트
7. 한계와 다음 단계

## 5. 대표 시나리오 규칙

설명에는 반드시 대표 시나리오 1개를 끝까지 포함하는 편이 좋다.

추천 시나리오:
- 위험한 채팅 입력
- `risk-score` 선판단
- `SAFETY` 응답 분기
- assistant 메시지 저장
- 이후 세션 조회/리포트 집계 가능성 설명

설명 시 포함하면 좋은 것:
- sequence diagram
- 실제 응답 예시
- 어떤 필드가 저장되는지
- AI 실패 시 fallback이 어떻게 유지되는지

## 6. 현재 저장소 기준 개선 우선순위

프로젝트 설명과 저장소 신뢰도를 높이기 위해 아래 개선 방향을 지속적으로 반영한다.

### 6-1. README 첫 화면을 문제 중심으로 다시 쓴다
README 첫 문장은 기술 스택이 아니라 문제 정의여야 한다.

좋은 예:
- 멘탈 헬스 기록/상담 서비스에서 AI가 실패해도 기록 저장과 safety 분기는 유지되어야 한다.

그 다음에 3계층 구조와 책임 분리를 설명한다.

### 6-2. 문서를 current / legacy로 나눈다
특히 아래 문서는 현재 운영 경로와 과거 비교 경로를 섞지 않도록 주의한다.

- `docs/README.md`
- `docs/ai-api/RISK_SCORE_API_LEARNING.md`
- `docs/ai-api/ANALYZE_DIARY_API_LEARNING.md`
- `docs/ai-api/GENERATE_REPLY_API_LEARNING.md`

원칙:
- 현재 운영 구조는 current로 설명
- 과거 비교 흔적은 legacy 또는 comparison으로 분리
- current 문서에서 혼합 서술을 줄인다

### 6-3. 내부 AI 호출 hardening을 설명과 코드에 반영한다
현재 설계 의도상 `backend-api -> ai-api` 경로에는 아래 요소가 중요하다.

- 짧은 timeout
- fallback 응답
- 실패 메트릭 증가
- diary/chat 핵심 흐름 유지

README나 설명문에서도 이 지점을 설계 강점 또는 남은 개선 과제로 정직하게 언급한다.

### 6-4. 한 장짜리 scorecard를 만든다
프로젝트를 수치로 설명할 때는 확인 가능한 지표만 사용한다.

추천 항목:
- diary create success under AI failure
- chat safety branch recall on curated samples
- fallback 발생률
- 응답 p95
- 모델 승격 기준

중요:
- 아직 없는 지표는 이미 운영 중인 것처럼 쓰지 않는다
- 대신 "정리 예정" 또는 "다음 단계"로 구분한다

### 6-5. 실험 산출물은 제품 설명과 분리한다
repo에는 요약본과 핵심 근거만 남기고, 대용량 artifacts / logs / 임시 산출물은 분리하는 편이 좋다.

이유:
- 제품 프로젝트의 중심 메시지가 선명해진다
- README와 문서가 연구 노트처럼 보이지 않는다
- 면접관이 핵심 구조를 더 빨리 이해할 수 있다

## 7. 검증 포인트를 쓰는 법

검증 포인트는 감상평이 아니라 사실 기반으로 쓴다.

좋은 예:
- diary create 시 AI 분석 실패가 나도 저장이 유지됨
- chat send-message 시 `SAFETY`, `SUPPORTIVE`, `NORMAL`, `FALLBACK` 분기 확인
- 공개 API E2E 테스트에서 fallback 회귀 방지 검증
- 모델 registry와 serving runtime 정보 정합성 확인 경로 존재

나쁜 예:
- 안정적이다
- 성능이 좋다
- 실서비스 수준이다

위 문장들은 근거 없이는 쓰지 않는다.

## 8. 이력서/포트폴리오 문체 규칙

문체는 아래를 지킨다.

- 한국어
- 발표자료 톤보다 실제 취업용 문체
- 짧지만 설계 의도가 분명하게 보이게 작성
- "문제 -> 해결 방식 -> 검증 포인트 -> 남은 과제" 흐름 유지

추천 표현:
- 설계했다
- 분리했다
- 선판단하도록 구성했다
- 실패가 전체 흐름으로 번지지 않도록 했다
- fallback을 유지했다
- E2E 기준으로 검증했다

피해야 할 표현:
- 혁신적인
- 완벽한
- 고도화된 AI 플랫폼
- 업계 최고 수준
- 수치 근거 없는 성능 자랑

## 9. 최종 체크리스트

README, 포트폴리오 문구, 프로젝트 소개문을 쓰기 전 마지막으로 아래를 확인한다.

- 첫 문장이 문제 정의인가
- 구조 설명이 문제 설명보다 앞서지 않았는가
- current 구조와 legacy 설명이 섞이지 않았는가
- safety / fallback / persistence 보호 흐름이 드러나는가
- 검증 포인트가 사실 기반인가
- 없는 지표를 만들어 쓰지 않았는가
- 남은 한계도 정직하게 적었는가
