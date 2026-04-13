# Mind Compass

멘탈 헬스 서비스에서 AI가 실패하더라도 기록 저장과 안전 분기가 유지되도록 설계한 Safety-first 백엔드 프로젝트입니다.

Mind Compass는 감정 기록, 캘린더 조회, 상담형 채팅을 하나의 사용자 흐름으로 연결하면서도, 공개 비즈니스 API와 내부 AI
계층을 분리해 운영 안정성과 실험 가능성을 함께 가져가도록 설계했습니다.

---

## Overview

현재 구조는 아래와 같습니다.

`Responsive Web -> backend-api -> ai-api -> ai-api-fastapi`

- `backend-api`: 공개 API, 인증, diary/chat/calendar/report 저장 및 조회
- `ai-api`: 감정 분석, 위험도 판단, 답변 생성을 조율하는 내부 AI 오케스트레이터
- `ai-api-fastapi`: 감정 분류 모델 추론과 모델 runtime 정보를 제공하는 모델 서빙 계층

핵심 원칙은 단순합니다.

- 웹 클라이언트는 `backend-api`만 호출합니다.
- 위험도 판단은 일반 답변 생성보다 먼저 수행합니다.
- AI가 실패해도 diary/chat 핵심 흐름은 최대한 유지합니다.
- 모델 실험 경로와 공개 API 경로를 분리합니다.


---

## Architecture

```

  ┌─────────────────────────────────────────────────────────────────┐
  │                        Responsive Web                            │
  │                    (Next.js + Tailwind CSS)                      │
  └─────────────────────────────────────────────────────────────────┘
                                   │
                                   │ HTTP (public)
                                   ▼
  ┌─────────────────────────────────────────────────────────────────┐
  │                         backend-api                              │
  │                    (Spring Boot - Java)                          │
  │  Auth │ Diary │ Calendar │ Chat │ Report                        │
  └───────────────────────────────────┬─────────────────────────────┘
                                      │ HTTP (internal)
                                      ▼
  ┌─────────────────────────────────────────────────────────────────┐
  │                           ai-api                                 │
  │                    (Spring AI - Java)                            │
  │  Diary Analyzer │ Risk Scorer │ Reply Generator                  │
  └──────────────┬──────────────────────────────────────────────────┘
                 │ HTTP (internal)             │ HTTP (external)
                 ▼                             ▼
  ┌──────────────────────────┐     ┌────────────────────────────────┐
  │    ai-api-fastapi         │     │       LLM Provider             │
  │    (FastAPI - Python)     │     │    (OpenAI / Claude)          │
  │  Emotion Classifier       │     └────────────────────────────────┘
  └──────────────────────────┘
 
```
### 호출 흐름 요약

```
Responsive Web → backend-api → ai-api → ai-api-fastapi
                                     → LLM Provider
```

모든 외부 요청은 `backend-api`로 들어온다. AI가 필요한 요청만 내부적으로 `ai-api`를 거친다.

---


## Why This Project Exists

멘탈 헬스 도메인에서는 "좋은 AI 답변"보다 먼저 해결해야 하는 문제가 있었습니다.

1. AI 장애가 발생하면 diary/chat 저장 흐름까지 함께 깨질 수 있습니다.
2. 위험 신호 판단과 일반 답변 생성이 한 흐름에 섞이면 안전 분기가 늦어질 수 있습니다.
3. 클라이언트가 AI 구현에 직접 의존하면 구조가 빠르게 복잡해집니다.

이 프로젝트는 이 문제를 해결하기 위해 공개 API와 내부 AI를 분리하고, Safety-first 흐름을 중심으로 전체 구조를
설계했습니다.

---

## Core Design

### 1. Public API and AI Separation

`backend-api`는 서비스의 단일 공개 진입점입니다.

- Auth / JWT / User
- Diary CRUD
- Calendar / Report 조회
- Chat session / message 저장
- AI 결과 저장 및 응답 DTO 조립

`ai-api`는 내부 AI 오케스트레이터입니다.

- `analyze-diary`
- `risk-score`
- `generate-reply`
- prompt / fallback / safety / future RAG orchestration

`ai-api-fastapi`는 감정 분류 모델 서빙 계층입니다.

- emotion classification inference
- model version / threshold / calibration
- runtime metadata exposure

이 구조 덕분에 클라이언트는 단일 API만 알면 되고, 내부 AI 구현이 바뀌어도 외부 계약은 비교적 안정적으로 유지할 수
있습니다.

### 2. Safety-first Chat Flow

채팅은 바로 답변을 생성하지 않습니다.

1. 사용자 메시지를 먼저 저장합니다.
2. `risk-score`로 위험도를 먼저 판단합니다.
3. `HIGH`면 일반 답변 대신 `SAFETY` 응답으로 분기합니다.
4. `MEDIUM`이면 `SUPPORTIVE` 응답으로 분기합니다.
5. 그 외에만 `generate-reply`를 호출합니다.
6. AI 호출이 실패해도 fallback assistant 메시지를 저장해 대화가 끊기지 않게 합니다.

즉, 이 프로젝트의 채팅 흐름은 생성 품질 우선이 아니라 안전 분기 우선입니다.

### 3. Diary-first Persistence

일기 저장도 AI 성공을 전제로 하지 않습니다.

1. diary 레코드를 먼저 저장합니다.
2. 감정 분석과 위험도 분석은 후처리로 붙입니다.
3. 분석이 실패해도 `201 Created`는 유지합니다.
4. 성공한 AI 결과만 감정 태그, 요약, 위험도 필드에 반영합니다.

이렇게 해서 AI 장애가 기록 손실로 이어지지 않도록 했습니다.

---

## Implemented Scope

### backend-api
- Auth: `POST /signup`, `POST /login`, `POST /refresh`
- User: `GET /users/me`
- Diary: `POST`, `GET /{id}`, `PATCH /{id}`, `DELETE /{id}`, `GET ?date=`
- Calendar: `GET /monthly-emotions`, `GET /daily-summary`
- Chat: `POST /sessions`, `GET /sessions`, `GET /sessions/{id}`, `POST /sessions/{id}/messages`
- Report: `GET /monthly-summary`, `GET /emotions/weekly`, `GET /risks/monthly`

### ai-api
- `GET /health`
- `POST /internal/ai/analyze-diary`
- `POST /internal/ai/risk-score`
- `POST /internal/ai/generate-reply`

### ai-api-fastapi
- `GET /health`
- model runtime / serving information
- emotion classification serving and fallback policy exposure

---

## Validation

현재 프로젝트에서는 단순 CRUD 성공보다 아래 검증을 더 중요하게 다뤘습니다.

- Diary create 시 AI 분석 실패가 발생해도 저장이 유지되는가
- Chat send-message 시 `SAFETY`, `SUPPORTIVE`, `NORMAL`, `FALLBACK` 분기가 회귀 없이 유지되는가
- 공개 API E2E 테스트에서 fallback 흐름이 깨지지 않는가
- 모델 registry 정보와 serving runtime 정보를 비교할 수 있는가

테스트 범위에는 Service, Controller, Security, Ownership, Public API E2E가 포함됩니다.

---



## Local Development

일상 개발에서는 OpenAI API를 호출하지 않습니다. mock 응답과 fixture를 사용하며, 실제 API 호출은 프롬프트 변경 검증이나
QA 시에만 수동으로 수행합니다.

```bash
  # 1. database
  docker-compose up -d postgres

  # 2. ai-api-fastapi
  cd ai-api-fastapi && uvicorn main:app --port 8090

  # 3. ai-api
  cd ai-api && ./gradlew bootRun

  # 4. backend-api
  cd backend-api && ./gradlew bootRun

  # 5. web-app
  cd web-app && npm run dev
```
각 서버는 다른 서버 없이도 기본 기능이 동작합니다. AI 분석 결과는 null로 저장됩니다.

---

## Frontend Integration
web-app은 Next.js 기반 웹 클라이언트 골격이며, calendar/chat/report/diary/login 페이지와 backend-api 호출 클라이언트가 연결된 상태입니다.

- 웹은 backend-api만 호출합니다.
- ai-api, ai-api-fastapi는 내부 계층으로만 사용합니다.

---

## What I Wanted to Emphasize

이 프로젝트는 단순히 AI 기능을 붙여본 프로젝트가 아닙니다.

1. 공개 비즈니스 API와 내부 AI 실험 경로를 분리한 구조 설계
2. 멘탈 헬스 도메인에 맞춘 Safety-first fallback 흐름 설계

즉, "AI를 잘 쓰는 서비스"보다 먼저
"AI가 흔들려도 핵심 사용자 흐름이 무너지지 않는 서비스"를 만드는 데 초점을 맞췄습니다.

---

## Limitations and Next Steps

- 내부 AI 호출 timeout / retry / circuit-breaker 보강
- 응답 시간, fallback 발생률, safety 분기 적중률 같은 운영 지표 scorecard 정리
- 제품 경로와 실험 경로 문서 분리 정리

자세한 학습 문서와 API 명세는 ./docs/README.md를 참고하세요.
---
