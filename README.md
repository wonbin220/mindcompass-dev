# Mind Compass

감정 일기와 AI 상담을 결합한 멘탈 헬스 서비스의 백엔드 학습 프로젝트다.

이 README는 단순 소개가 아니라, 프로젝트를 처음 보는 junior backend developer가 구조를 이해하고 왜 이렇게 설계했는지 배울 수 있도록 작성했다.

---

## 목차

1. [프로젝트 목표](#1-프로젝트-목표)
2. [왜 이 구조로 가는가](#2-왜-이-구조로-가는가)
3. [전체 아키텍처](#3-전체-아키텍처)
4. [서버별 역할](#4-서버별-역할)
5. [왜 frontend는 backend-api만 호출하는가](#5-왜-frontend는-backend-api만-호출하는가)
6. [왜 ai-api와 ai-api-fastapi를 분리하는가](#6-왜-ai-api와-ai-api-fastapi를-분리하는가)
7. [로컬 실행 원칙](#7-로컬-실행-원칙)
8. [Zero-cost Dev vs Manual OpenAI Verification](#8-zero-cost-dev-vs-manual-openai-verification)
9. [MVP 우선순위](#9-mvp-우선순위)
10. [Safety Net이 왜 중요한가](#10-safety-net이-왜-중요한가)
11. [AI 비교 실험 기준](#11-ai-비교-실험-기준)
12. [구현 순서 가이드](#12-구현-순서-가이드)
13. [문서 안내](#13-문서-안내)

---

## 1. 프로젝트 목표

### 서비스 목표

사용자가 매일 감정 일기를 쓰고, AI가 감정을 분석하고, 필요할 때 상담 대화를 나눌 수 있는 멘탈 헬스 서비스를 만든다.

### 학습 목표

이 프로젝트는 실제 서비스를 만드는 것과 동시에 학습을 목표로 한다.

- Spring Boot로 REST API 서버를 만드는 방법
- AI 기능을 안전하게 서비스에 통합하는 방법
- 멀티 서버 아키텍처에서 책임을 나누는 방법
- 멘탈 헬스 서비스에서 safety를 다루는 방법

코드를 작성할 때마다 "왜 이렇게 하는가"를 함께 설명한다.

---

## 2. 왜 이 구조로 가는가

### 문제 상황

AI 기능이 포함된 서비스를 만들 때 흔히 겪는 문제가 있다.

1. AI 코드와 비즈니스 코드가 섞여서 유지보수가 어렵다
2. AI 서버가 죽으면 전체 서비스가 죽는다
3. AI 실험을 하려면 서비스 코드를 건드려야 한다
4. Python ML 코드와 Java 비즈니스 코드가 한 곳에 있어서 배포가 복잡하다

### 해결 방향

서버를 역할별로 나눈다.

- **backend-api**: 비즈니스 로직과 저장을 담당
- **ai-api**: AI 관련 정책과 흐름 제어를 담당
- **ai-api-fastapi**: ML 모델 추론만 담당

이렇게 나누면 각 서버가 독립적으로 발전할 수 있다. AI 모델을 바꿔도 비즈니스 로직은 영향받지 않는다. 비즈니스 로직을 수정해도 AI 모델 서빙은 그대로다.

### Tradeoff

장점:
- 관심사 분리가 명확하다
- 각 서버를 독립 배포할 수 있다
- AI 실패가 전체 서비스를 죽이지 않는다

단점:
- 서버가 3개라서 로컬 개발 환경이 복잡하다
- 서버 간 통신 오버헤드가 있다
- 초기 설정 비용이 높다

이 프로젝트에서는 장점이 단점보다 크다고 판단했다. 특히 멘탈 헬스 서비스에서 AI 실패로 인한 전체 서비스 중단은 피해야 하기 때문이다.

---

## 3. 전체 아키텍처

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
│                                                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │   Auth   │ │  Diary   │ │ Calendar │ │   Chat   │           │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
│                                                                  │
│                    ┌──────────────────┐                         │
│                    │  AI Client       │ ─────────┐              │
│                    └──────────────────┘          │              │
└──────────────────────────────────────────────────│──────────────┘
                                                   │
                                                   │ HTTP (internal)
                                                   ▼
┌─────────────────────────────────────────────────────────────────┐
│                           ai-api                                 │
│                    (Spring AI - Java)                            │
│                                                                  │
│  ┌────────────────┐ ┌────────────────┐ ┌────────────────┐      │
│  │ Diary Analyzer │ │ Risk Scorer    │ │ Reply Generator│      │
│  └────────────────┘ └────────────────┘ └────────────────┘      │
│                                                                  │
│  ┌────────────────┐ ┌────────────────┐                         │
│  │ RAG Context    │ │ Safety Policy  │ ─────────┐              │
│  └────────────────┘ └────────────────┘          │              │
└─────────────────────────────────────────────────│───────────────┘
                    │                             │
                    │ HTTP (internal)             │ HTTP (internal)
                    ▼                             ▼
┌───────────────────────────┐     ┌───────────────────────────────┐
│     ai-api-fastapi        │     │       LLM Provider            │
│    (FastAPI - Python)     │     │    (OpenAI / Claude)          │
│                           │     │                               │
│  ┌─────────────────────┐  │     └───────────────────────────────┘
│  │ Emotion Classifier  │  │
│  └─────────────────────┘  │
│                           │
│  ┌─────────────────────┐  │
│  │ Model Version Mgmt  │  │
│  └─────────────────────┘  │
└───────────────────────────┘
```

### 호출 흐름 요약

```
Responsive Web → backend-api → ai-api → ai-api-fastapi
                                     → LLM Provider
```

모든 외부 요청은 `backend-api`로 들어온다. AI가 필요한 요청만 내부적으로 `ai-api`를 거친다.

---

## 4. 서버별 역할

### 4-1. backend-api (Spring Boot)

**한 줄 요약**: 웹 서비스의 공개 API 진입점이자 비즈니스 로직의 중심

담당하는 것:
- **Auth**: 회원가입, 로그인, JWT 발급 및 검증
- **User**: 사용자 정보 관리
- **Diary**: 일기 저장, 조회, 수정, 삭제
- **Calendar**: 날짜별/기간별 일기 조회
- **Chat**: 채팅 세션 생성, 메시지 저장
- **Report**: 주간/월간 감정 리포트
- **AI 결과 저장**: ai-api가 반환한 분석 결과를 DB에 저장

담당하지 않는 것:
- 프롬프트 작성
- RAG 검색
- 감정분류 모델 호출
- LLM 직접 호출

**왜 이렇게 나누는가?**

비즈니스 로직과 AI 로직을 섞으면 나중에 분리하기 어렵다. backend-api는 "저장하고 조회하는 것"에 집중하고, AI 관련 복잡성은 ai-api에 위임한다.

### 4-2. ai-api (Spring AI)

**한 줄 요약**: AI 기능의 정책과 흐름을 제어하는 오케스트레이터

담당하는 것:
- **일기 감정 분석**: 일기 텍스트에서 감정을 추출
- **위험도 점수 계산**: 텍스트의 위기 신호를 감지
- **AI 답변 생성**: 채팅에서 공감 답변 생성
- **RAG Context 조립**: 과거 대화/일기에서 관련 문맥 검색
- **Safety Policy**: 위기 상황 판단 및 분기
- **ai-api-fastapi 호출**: 감정분류 모델 추론 요청
- **LLM Provider 호출**: OpenAI/Claude API 호출

담당하지 않는 것:
- 사용자 인증/인가
- DB 직접 접근 (저장은 backend-api가 함)
- PyTorch 모델 직접 로딩

**왜 Spring AI를 쓰는가?**

Spring AI는 LLM 호출, 프롬프트 템플릿, 임베딩 처리를 추상화해준다. backend-api와 같은 Java/Spring 생태계를 쓰면 팀 내 기술 스택 통일이 쉽다.

대안으로 LangChain(Python)을 쓸 수도 있지만, 그러면 Java 개발자가 Python 코드까지 관리해야 한다. 이 프로젝트에서는 ML 추론만 Python으로 분리하고, 나머지는 Java로 통일했다.

### 4-3. ai-api-fastapi (FastAPI)

**한 줄 요약**: 감정분류 모델을 서빙하는 전용 계층

담당하는 것:
- **감정분류 추론**: 텍스트 입력 → 감정 레이블 출력
- **모델 버전 관리**: v1, v2 모델 전환
- **Threshold/Calibration**: 확률 임계값 조정
- **실험 라우팅**: A/B 테스트용 모델 분기
- **추론 메타데이터 반환**: 확률값, 모델 버전, 처리 시간

담당하지 않는 것:
- 사용자 메모리/대화 히스토리 관리
- 상담 멘트 생성
- RAG 검색
- 최종 AI 응답 조합

**왜 FastAPI를 쓰는가?**

PyTorch 기반 감정분류 모델은 Python 환경에서 돌아간다. Python ML 생태계(transformers, torch, numpy)를 그대로 쓰려면 Python 서버가 필요하다.

FastAPI는 Python 웹 프레임워크 중 가장 빠르고, 타입 힌트 기반으로 자동 문서화가 된다. ML 모델 서빙에 적합하다.

---

## 5. 왜 frontend는 backend-api만 호출하는가

### 원칙

```
✅ frontend → backend-api
❌ frontend → ai-api
❌ frontend → ai-api-fastapi
```

### 이유 1: 보안

ai-api와 ai-api-fastapi는 내부 서버다. 외부에 노출하면 직접 공격 대상이 된다. backend-api 한 곳에서만 인증/인가를 처리하면 보안 정책을 일관되게 적용할 수 있다.

### 이유 2: 안정성

frontend가 ai-api를 직접 호출하면, ai-api가 죽을 때 frontend도 영향받는다. backend-api를 거치면 AI 실패를 graceful하게 처리할 수 있다.

```java
// backend-api의 fallback 예시
try {
    EmotionResult result = aiApiClient.analyzeEmotion(text);
    diary.setEmotion(result.getEmotion());
} catch (AiApiException e) {
    // AI 실패해도 일기는 저장
    diary.setEmotion(null);
    log.warn("AI 분석 실패, 일기는 감정 없이 저장", e);
}
diaryRepository.save(diary);
```

### 이유 3: 변경 격리

AI 서버 구조가 바뀌어도 frontend는 모른다. backend-api만 수정하면 된다.

예를 들어 ai-api-fastapi를 나중에 AWS SageMaker로 교체해도, frontend 코드는 한 줄도 바뀌지 않는다.

### 이유 4: 단순성

frontend 개발자는 "backend-api 엔드포인트만 알면 된다"가 된다. AI 내부 구조를 몰라도 서비스를 만들 수 있다.

---

## 6. 왜 ai-api와 ai-api-fastapi를 분리하는가

### 자주 나오는 질문

> "ai-api 하나에서 LLM 호출도 하고 감정분류도 하면 안 되나?"

### 대답: 분리하는 편이 낫다

### 이유 1: 언어/생태계 차이

- LLM 호출, 프롬프트 관리, RAG → Java/Spring AI로 충분
- PyTorch 감정분류 모델 → Python 필수

한 서버에 Java와 Python을 섞으면 배포와 의존성 관리가 복잡해진다.

### 이유 2: 배포 단위 분리

감정분류 모델을 업데이트해도 ai-api는 재배포 안 해도 된다. 모델 버전만 ai-api-fastapi에서 바꾸면 된다.

### 이유 3: 스케일링 단위 분리

감정분류 요청이 많아지면 ai-api-fastapi만 늘리면 된다. LLM 호출이 많아지면 ai-api만 늘리면 된다.

### 이유 4: 실험 격리

새 감정분류 모델을 실험할 때 ai-api-fastapi 내부에서만 A/B 테스트하면 된다. ai-api 코드를 건드릴 필요 없다.

### Tradeoff

단점도 있다:
- 서버가 하나 더 있으니 운영 복잡도 증가
- 서버 간 통신 latency 추가
- 로컬 개발 시 서버 3개 띄워야 함

하지만 ML 모델과 LLM 오케스트레이션을 분리하는 것은 업계 표준에 가까운 패턴이다. 장기적으로 유지보수가 쉬워진다.

---

## 7. 로컬 실행 원칙

### 기본 원칙: 서버를 독립적으로 실행할 수 있어야 한다

각 서버는 다른 서버 없이도 기본 기능이 동작해야 한다.

```
# backend-api만 실행
./gradlew :backend-api:bootRun

# ai-api 없어도 일기 CRUD는 동작
# 단, AI 분석 결과는 null로 저장됨
```

### Mock 서버 활용

로컬에서 ai-api-fastapi 없이 개발할 때는 mock 응답을 쓴다.

```yaml
# backend-api의 application-local.yml
ai:
  api:
    enabled: false
    mock-response: true
```

### 환경별 프로필

```
local   → mock AI, H2 DB
dev     → 실제 AI, dev DB
staging → 실제 AI, staging DB
prod    → 실제 AI, prod DB
```

### 실행 순서 (전체 통합 테스트 시)

```bash
# 1. 데이터베이스
docker-compose up -d postgres

# 2. ai-api-fastapi (Python)
cd ai-api-fastapi
python -m uvicorn main:app --port 8090

# 3. ai-api (Spring AI)
cd ai-api
./gradlew bootRun

# 4. backend-api (Spring Boot)
cd backend-api
./gradlew bootRun

# 5. web-app (Next.js)
cd web-app
npm run dev
```

---

## 8. Zero-cost Dev vs Manual OpenAI Verification

### Zero-cost Dev 원칙

일상적인 개발에서는 OpenAI API를 호출하지 않는다.

이유:
- API 비용이 쌓인다
- rate limit에 걸릴 수 있다
- 네트워크 상태에 따라 테스트가 불안정해진다

대신:
- mock 응답을 쓴다
- 저장된 fixture를 쓴다
- 로컬 임베딩 모델을 쓴다 (필요시)

```java
// 테스트에서 mock 사용 예시
@MockBean
private OpenAiClient openAiClient;

@Test
void 일기_분석_테스트() {
    given(openAiClient.chat(any()))
        .willReturn(new ChatResponse("기쁨"));

    // 실제 OpenAI 호출 없이 테스트
}
```

### Manual OpenAI Verification

실제 OpenAI API 품질을 확인해야 할 때만 수동으로 호출한다.

시점:
- 프롬프트를 변경했을 때
- 새 모델(GPT-4 → GPT-4o)로 전환할 때
- QA 전 품질 검증할 때

방법:
```bash
# 수동 검증 스크립트 실행
cd scripts
python manual_openai_check.py --prompt diary_analysis --count 10
```

결과는 `docs/ai-api/MANUAL_OPENAI_QUALITY_CHECKLIST.md`에 기록한다.

### 요약

| 상황 | OpenAI 호출 | 방법 |
|------|------------|------|
| 일상 개발 | ❌ | mock/fixture |
| 단위 테스트 | ❌ | mock |
| 통합 테스트 | ❌ | mock 또는 fixture |
| 프롬프트 변경 검증 | ✅ | 수동 스크립트 |
| QA 검증 | ✅ | staging 환경 |
| 프로덕션 | ✅ | 실제 호출 |

---

## 9. MVP 우선순위

아래 순서로 구현한다. 각 단계는 이전 단계가 완성되어야 진행한다.

### Phase 1: Foundation

1. **Spring Boot 프로젝트 구조**
   - Gradle 멀티 모듈 설정
   - 공통 설정, 예외 처리, 응답 포맷

2. **Auth / User**
   - 회원가입 (이메일, 비밀번호)
   - 로그인 (JWT 발급)
   - 토큰 검증

### Phase 2: Core Features

3. **Diary CRUD**
   - 일기 저장 (제목, 본문, 날짜)
   - 일기 조회 (단건, 목록)
   - 일기 수정, 삭제

4. **Calendar / Emotion Queries**
   - 날짜별 일기 조회
   - 월간 일기 목록
   - 감정별 필터링 (AI 연동 후)

### Phase 3: AI Integration

5. **Minimal ai-api Endpoints**
   - 일기 감정 분석 엔드포인트
   - 위험도 점수 엔드포인트

6. **Chat Sessions / Messages**
   - 채팅 세션 생성
   - 메시지 저장
   - AI 답변 생성 연동

### Phase 4: Safety & Insights

7. **Safety Net**
   - 위기 신호 감지
   - 안전 분기 처리
   - 긴급 연락처 안내

8. **Reports / Statistics**
   - 주간 감정 요약
   - 월간 리포트

### Phase 5: Advanced

9. **Advanced AI Features**
   - RAG 기반 문맥 검색
   - 대화 메모리 개선
   - 고급 프롬프트 전략

---

## 10. Safety Net이 왜 중요한가

### 멘탈 헬스 서비스의 특수성

이 서비스는 감정적으로 취약한 상태의 사용자가 쓴다. 일반 앱과 다른 고려가 필요하다.

### Safety Net이란?

사용자의 텍스트에서 위기 신호(자해, 자살 언급 등)를 감지하고, 적절한 대응을 하는 안전 장치다.

### 왜 AI 품질보다 먼저인가

AI가 아무리 공감을 잘 해도, 위기 상황에서 엉뚱한 대답을 하면 심각한 문제가 된다.

우선순위:
```
1. Safety (위기 감지 및 적절한 대응) - 필수
2. Harmlessness (해가 되는 말 안 하기) - 필수
3. Helpfulness (도움이 되는 말 하기) - 중요
4. Engagement (대화가 자연스러움) - 좋으면 좋음
```

### 구현 원칙

1. **위기 감지는 AI 응답 생성 전에 한다**
   ```
   사용자 입력 → 위기 감지 → [위기면 안전 분기] → AI 응답 생성
   ```

2. **위기 감지가 실패해도 fallback이 있다**
   ```java
   try {
       RiskScore risk = aiApi.calculateRisk(text);
       if (risk.isHigh()) {
           return SafetyResponse.crisis();
       }
   } catch (Exception e) {
       // 위기 감지 실패 시 보수적으로 처리
       if (containsKeywords(text, CRISIS_KEYWORDS)) {
           return SafetyResponse.crisis();
       }
   }
   ```

3. **안전 응답은 하드코딩한다**
   위기 상황 응답은 AI가 생성하지 않는다. 검증된 고정 메시지를 쓴다.
   ```
   "지금 많이 힘드시군요. 전문 상담이 도움이 될 수 있어요.
   자살예방상담전화 1393, 정신건강위기상담전화 1577-0199"
   ```

4. **로깅은 반드시 한다**
   위기 감지 이벤트는 별도로 로깅해서 모니터링한다.

---

## 11. AI 비교 실험 기준

### 언제 비교 실험을 하는가

- 새 LLM 모델로 전환할 때 (GPT-3.5 → GPT-4)
- 프롬프트를 크게 변경할 때
- 새 감정분류 모델을 배포할 때

### 비교 기준

#### 1. Safety (안전성)

- 위기 상황에서 적절한 대응을 하는가?
- 해로운 조언을 하지 않는가?
- 민감한 주제에서 적절히 경계를 지키는가?

측정: 안전 테스트 케이스 통과율

#### 2. Accuracy (정확성)

- 감정 분류가 맞는가?
- 맥락을 제대로 이해하는가?
- hallucination이 없는가?

측정: 레이블된 테스트셋 정확도

#### 3. Latency (응답 속도)

- 사용자가 기다리기 불편하지 않은가?
- p50, p95, p99 latency

목표: p95 < 3초

#### 4. Cost (비용)

- 토큰 사용량
- API 호출 비용

측정: 요청당 평균 비용

#### 5. User Experience (사용자 경험)

- 응답이 자연스러운가?
- 공감이 느껴지는가?
- 대화 흐름이 매끄러운가?

측정: 내부 QA 평가, 사용자 피드백

### 실험 원칙

1. **서비스 안정성을 깨지 않는다**
   - 실험은 일부 트래픽에만 적용
   - 문제 발생 시 즉시 롤백

2. **A/B 테스트는 ai-api-fastapi 또는 ai-api 내부에서**
   - backend-api는 실험 로직을 모른다
   - frontend도 실험 로직을 모른다

3. **결과를 기록한다**
   ```markdown
   ## 실험: GPT-4o 전환
   기간: 2024-01-15 ~ 2024-01-22
   대상: 10% 트래픽
   결과:
   - Safety: 100% → 100% (동일)
   - Accuracy: 85% → 89% (개선)
   - Latency p95: 2.8s → 2.1s (개선)
   - Cost: $0.03 → $0.025 (절감)
   결론: 전환 승인
   ```

---

## 12. 구현 순서 가이드

### 추천 순서

```
1. backend-api 기반 작업
   └── Spring Boot 프로젝트 설정
   └── Auth API
   └── Diary API
   └── Calendar API

2. ai-api 기반 작업
   └── Spring AI 프로젝트 설정
   └── Diary Analyzer 엔드포인트
   └── backend-api와 연동

3. ai-api-fastapi 기반 작업
   └── FastAPI 프로젝트 설정
   └── Emotion Classifier 엔드포인트
   └── ai-api와 연동

4. Chat 기능
   └── backend-api: Chat Session/Message API
   └── ai-api: Reply Generator
   └── Safety Net 통합

5. Report 기능
   └── backend-api: Report API
   └── ai-api: 요약 생성 (선택)

6. 고급 기능
   └── RAG 구현
   └── 대화 메모리 개선
```

### 각 단계에서 확인할 것

- [ ] API가 화면의 어떤 동작과 연결되는가?
- [ ] 예외 상황에서 어떻게 동작하는가?
- [ ] AI 실패 시 fallback이 있는가?
- [ ] 테스트가 있는가?
- [ ] 문서가 업데이트되었는가?

---

## 13. 문서 안내

### 문서 구조

```
docs/
├── README.md                      # 문서 안내
├── AUTH_API_LEARNING.md           # Auth API 학습
├── DIARY_API_LEARNING.md          # Diary API 학습
├── CALENDAR_API_LEARNING.md       # Calendar API 학습
├── CHAT_API_LEARNING.md           # Chat API 학습
├── REPORT_API_LEARNING.md         # Report API 학습
├── DB_TABLE_SPECIFICATION.md      # DB 테이블 명세
├── SCREEN_TO_API_MAPPING.md       # 화면-API 매핑
├── IMPLEMENTATION_STATUS.md       # 진행 상황
├── BACKEND_AI_LOCAL_RUN_GUIDE.md  # 로컬 실행 가이드
├── OPERATIONS_GUIDE.md            # 운영 가이드
└── ai-api/
    ├── README.md                  # AI API 개요
    ├── AI_API_OVERVIEW_LEARNING.md
    ├── ANALYZE_DIARY_API_LEARNING.md
    ├── RISK_SCORE_API_LEARNING.md
    ├── GENERATE_REPLY_API_LEARNING.md
    └── ...
```

### 권장 읽기 순서

1. 이 `README.md`
2. `docs/README.md`
3. `docs/DB_TABLE_SPECIFICATION.md`
4. `docs/SCREEN_TO_API_MAPPING.md`
5. 작업할 도메인의 학습 문서
6. `docs/ai-api/README.md` (AI 작업 시)

### 기여 규칙

- 새 API를 만들면 해당 도메인 학습 문서 업데이트
- 진행 상황은 `IMPLEMENTATION_STATUS.md`에 기록
- AI 에이전트 규칙은 `AGENTS.md` 참고

---

## 라이선스

이 프로젝트는 학습 목적으로 작성되었다.

---

## 문의

이 프로젝트에 대한 질문이나 제안은 이슈로 등록해주세요.
