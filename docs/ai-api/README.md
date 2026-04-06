# Mind Compass ai-api 문서 모음

이 폴더는 Mind Compass의 AI 계층 구조와 내부 AI API를 이해하기 위한 문서를 모아둔 곳이다.

현재 프로젝트는 AI 서버를 2개로 나누되, 병렬 공개 서버가 아니라 계층이 분명한 구조를 목표로 한다.

- `backend-api` = 공개 비즈니스 API 서버
- `ai-api` = AI 오케스트레이터 서버
- `ai-api-fastapi` = 감정분류 모델 서빙 서버

핵심 호출 흐름은 아래와 같다.

`Responsive Web -> backend-api -> ai-api -> ai-api-fastapi`

즉, 웹 클라이언트는 `backend-api`만 호출하고, AI 관련 내부 진입점도 가능하면 `ai-api` 하나로 고정한다.

---

## 왜 이렇게 나누는가

이 구조는 단순히 서버를 많이 두려는 목적이 아니라, 관심사를 분리하기 위한 설계다.

### 1. `backend-api`

담당:

- 인증 / 인가
- 회원, 일기, 채팅 세션, 기록 CRUD
- 캘린더 / 리포트 조회
- 웹 화면용 REST API
- AI 결과 저장 / 조회

하지 않는 것:

- 프롬프트 조립
- RAG 문맥 구성
- 감정분류 모델 직접 호출
- LLM 직접 호출

### 2. `ai-api`

`ai-api`는 단순 추론 서버가 아니라 AI 애플리케이션 레벨 오케스트레이터다.

담당:

- 프롬프트 템플릿 관리
- 대화 메모리 조립
- RAG retrieval + context building
- 어떤 AI 기능을 언제 호출할지 판단
- `ai-api-fastapi` 호출
- LLM Provider 호출
- 최종 응답 조합
- safety / fallback / retry

하지 않는 것:

- 회원 / 권한 / 일반 CRUD
- 웹 클라이언트 공개 API 제공
- PyTorch 모델 직접 로딩

### 3. `ai-api-fastapi`

`ai-api-fastapi`는 "비교용 서버"라는 표현보다 "감정분류 모델 서빙 계층"으로 이해하는 편이 정확하다.

담당:

- 감정분류 추론
- 모델 버전 관리
- threshold / calibration
- 실험 라우팅
- 추론 메타데이터 반환

하지 않는 것:

- 사용자 메모리 해석
- 상담 멘트 생성
- RAG
- 최종 응답 조합

---

## 왜 `backend-api`가 FastAPI를 직접 호출하지 않게 하는가

이 원칙이 중요하다.

- AI 흐름의 진입점이 하나라서 구조가 단순해진다.
- 프롬프트 / 메모리 / safety 정책이 `ai-api`에 모인다.
- 모델 서빙 계층 교체가 쉬워진다.
- 웹 서비스 비즈니스 로직과 ML 추론 로직이 섞이지 않는다.

권장 흐름:

1. 웹이 `backend-api`로 요청한다.
2. `backend-api`가 저장, 권한, 세션, 비즈니스 검증을 처리한다.
3. AI가 필요하면 `ai-api`를 호출한다.
4. `ai-api`가 필요에 따라 `ai-api-fastapi`, RAG store, LLM Provider를 호출한다.
5. `ai-api`가 최종 구조화 응답을 `backend-api`에 돌려준다.
6. `backend-api`가 결과를 저장하거나 화면용 응답으로 가공한다.

---

## 문서 목록

- `AI_API_OVERVIEW_LEARNING.md`
- `AI_API_FASTAPI_SERVING_CONTRACT.md`
- `ANALYZE_DIARY_API_LEARNING.md`
- `RISK_SCORE_API_LEARNING.md`
- `GENERATE_REPLY_API_LEARNING.md`
- `RAG_CONTEXT_API_LEARNING.md`
- `MARGIN_LOSS_V3_AND_NEXT_SELECTION_RULE.md`
- `OPENAI_USAGE_AND_PROFILE_GUIDE.md`
- `MANUAL_OPENAI_QUALITY_CHECKLIST.md`
- `INTERNAL_API_SPEC_DRAFT.md`
- `AI_API_LOGICAL_ERD.md`

---

## 권장 읽는 순서

1. `AI_API_OVERVIEW_LEARNING.md`
2. `AI_API_FASTAPI_SERVING_CONTRACT.md`
3. `INTERNAL_API_SPEC_DRAFT.md`
4. `ANALYZE_DIARY_API_LEARNING.md`
5. `RISK_SCORE_API_LEARNING.md`
6. `GENERATE_REPLY_API_LEARNING.md`
7. `OPENAI_USAGE_AND_PROFILE_GUIDE.md`
8. `MANUAL_OPENAI_QUALITY_CHECKLIST.md`
9. `RAG_CONTEXT_API_LEARNING.md`
10. `AI_API_LOGICAL_ERD.md`
11. `MARGIN_LOSS_V3_AND_NEXT_SELECTION_RULE.md`

---

## 이름에 대한 메모

현재 저장소 이름은 아래와 같지만,

- `backend-api`
- `ai-api`
- `ai-api-fastapi`

문서 설명 관점에서는 아래 개념으로 이해하는 것이 더 명확하다.

- `backend-api` = public business API
- `ai-api` = AI orchestrator
- `ai-api-fastapi` = emotion model API

문서와 슬라이드에서는 이 개념명을 함께 병기해 혼동을 줄인다.
