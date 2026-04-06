# ai-api-fastapi 서빙 계약 정리 문서

이 문서는 `ai-api-fastapi`를 감정분류 모델 서빙 계층으로 사용할 때의 현재 계약을 한곳에 모아 정리한 문서다.

---

## 1. Goal

- `ai-api-fastapi`에서 어떤 엔드포인트를 실제 서빙 계약으로 봐야 하는지 고정한다.
- `ai-api`와 이후 연결할 때 어떤 요청/응답 필드를 기준으로 삼아야 하는지 정리한다.
- 학습 실험 경로와 실제 서빙 경로를 분리해서 설명한다.

---

## 2. Design Decision

현재 `ai-api-fastapi`에는 과거 비교용 라우터가 함께 남아 있다.
하지만 **실제 감정분류 서빙 계약의 기준 엔드포인트는 아래 하나로 본다.**

- `POST /internal/model/emotion-classify`

즉:

- `analyze-diary`
- `risk-score`
- `generate-reply`

같은 라우터가 FastAPI에 남아 있더라도,
감정분류 모델 서빙 계층으로서의 제품 계약은 `model_router.py` 기준으로 설명한다.

---

## 3. Related Files

- `C:\programing\mindcompass\ai-api-fastapi\app\main.py`
- `C:\programing\mindcompass\ai-api-fastapi\app\routers\model_router.py`
- `C:\programing\mindcompass\ai-api-fastapi\app\schemas\emotion_classify.py`
- `C:\programing\mindcompass\ai-api-fastapi\app\schemas\runtime_info.py`
- `C:\programing\mindcompass\ai-api-fastapi\app\services\emotion_classifier_service.py`
- `C:\programing\mindcompass\ai-api-fastapi\app\inference\predictor.py`
- `C:\programing\mindcompass\ai-api-fastapi\app\inference\label_mapper.py`
- `C:\programing\mindcompass\docs\ai-api\EMOTION_MODEL_PROMOTION_CHECKLIST.md`
- `C:\programing\mindcompass\docs\ai-api\KCELECTRA_TRAINING_LEARNING.md`

---

## 4. Current Serving Endpoints

### 4-1. Emotion classify

- `POST /internal/model/emotion-classify`

왜 필요한가:

- `ai-api`가 diary/chat 문장에서 감정 1차 추론 결과를 받아오기 위한 내부 모델 서빙 진입점이다.

요청:

```json
{
  "text": "오늘은 조금 불안했지만 그래도 버틸 수 있었어요.",
  "returnTopK": 3
}
```

응답:

```json
{
  "primaryEmotion": "ANXIOUS",
  "confidence": 0.8123,
  "emotionTags": ["ANXIOUS", "OVERWHELMED"],
  "scores": [
    {"label": "ANXIOUS", "score": 0.8123},
    {"label": "SAD", "score": 0.1031},
    {"label": "CALM", "score": 0.0542}
  ],
  "modelName": "beomi/KcELECTRA-base",
  "fallbackUsed": false,
  "fallbackReason": null
}
```

### 4-2. Runtime info

- `GET /internal/model/runtime-info`

왜 필요한가:

- 현재 FastAPI가 어떤 모델 경로, 라벨 계약, fallback 정책으로 서빙 중인지 운영자가 바로 확인하기 위해 필요하다.

현재 확인 가능한 핵심 필드:

- `modelDirConfigured`
- `modelDirResolved`
- `modelDirExists`
- `labelMapPathConfigured`
- `labelMapPathResolved`
- `labelMapPathExists`
- `modelName`
- `modelLoadSource`
- `maxLength`
- `labelCount`
- `servingPrimaryLabels`
- `fallbackPolicy`
- `topKMax`

---

## 5. Current Serving Contract Rules

### 5-1. Label contract

- 현재 label map 파일은 6-label을 유지한다.
  - `HAPPY`
  - `CALM`
  - `ANXIOUS`
  - `SAD`
  - `ANGRY`
  - `TIRED`
- 하지만 **서빙 primary label 계약은 5-label 중심**으로 본다.
  - `HAPPY`
  - `CALM`
  - `ANXIOUS`
  - `SAD`
  - `ANGRY`

### 5-2. TIRED fallback rule

- 모델이 `TIRED`를 1등으로 예측하면 그대로 외부 primary emotion으로 내보내지 않는다.
- 이 경우 FastAPI는 아래 보수 계약으로 응답한다.
  - `primaryEmotion = CALM`
  - `fallbackUsed = true`
  - `fallbackReason = "TIRED_FALLBACK_ONLY"`

왜 이렇게 하는가:

- 현재 문서와 체크리스트 기준으로 `TIRED`는 아직 안정적인 서빙 primary label로 승격되지 않았기 때문이다.

### 5-3. Empty / model error fallback

- 빈 문자열이면:
  - `primaryEmotion = CALM`
  - `fallbackUsed = true`
  - `fallbackReason = "EMPTY_TEXT"`
- 모델 로드/추론 예외면:
  - `primaryEmotion = CALM`
  - `fallbackUsed = true`
  - `fallbackReason = "MODEL_ERROR"`

---

## 6. Execution Flow

1. `ai-api`가 감정 추론이 필요할 때 `POST /internal/model/emotion-classify`를 호출한다.
2. `model_router.py`가 요청을 받는다.
3. `EmotionClassifyRequest`가 요청 body를 검증한다.
4. `EmotionClassifierService.classify_text()`가 predictor를 호출한다.
5. `predictor.py`가 모델 디렉터리 또는 base model에서 tokenizer/model을 로드한다.
6. softmax 결과에서 top-k를 계산한다.
7. `label_mapper.py`가 class index를 label/tag로 매핑한다.
8. `TIRED` 또는 예외 상황이면 fallback 응답으로 변환한다.
9. 구조화된 `EmotionClassifyResponse`를 `ai-api`에 반환한다.

---

## 7. Current Cautions

- `app/main.py`에는 과거 비교용 라우터가 아직 함께 포함되어 있다.
- 그래서 코드만 보면 FastAPI가 `analyze-diary`, `risk-score`, `generate-reply`까지 전부 맡는 것처럼 보일 수 있다.
- 하지만 제품 구조 기준에서는:
  - `ai-api` = 오케스트레이터
  - `ai-api-fastapi` = 감정분류 모델 서빙
- 즉 후속 정리 방향은 **FastAPI 제품 계약을 `emotion-classify` 중심으로 더 좁게 설명하는 것**이다.

---

## 8. Integration Rule For ai-api

`ai-api`가 FastAPI 결과를 메인 추론 소스로 붙일 때 최소한 아래 필드를 신뢰 계약으로 유지한다.

- `primaryEmotion`
- `emotionTags`
- `confidence`
- `fallbackUsed`
- `fallbackReason`

`scores`는 디버깅, shadow 비교, 운영 추적에는 유용하지만,
초기 diary/chat 화면 계약까지 그대로 올릴지는 `ai-api`에서 다시 결정한다.

---

## 9. Next Step

- `DiaryAnalysisService`가 현재 Spring-AI prompt 중심 경로에서,
  FastAPI `emotion-classify` 결과를 주된 감정 입력으로 읽는 구조로 점진 전환할지 결정한다.
- `runtime-info`를 기준으로 registry active row와 FastAPI runtime path alignment를 점검한다.
- FastAPI에 남아 있는 과거 비교용 라우터와, 제품 서빙 계약 라우터를 문서/테스트에서 계속 분리해 설명한다.
