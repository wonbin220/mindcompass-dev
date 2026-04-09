# 감정분류 모델 학습 작업 디렉터리 안내 문서입니다.

이 폴더는 `beomi/KcELECTRA-base` 기반 감정분류 MVP를 학습하고 서빙 검증까지 이어가기 위한 작업 디렉터리입니다.

권장 구조:

```text
training/emotion_classifier/
├─ raw/
├─ interim/
├─ processed/
├─ artifacts/
├─ configs/
├─ logs/
└─ scripts/
```

각 폴더 역할:

- `raw/`: 원본 데이터 복사본 또는 경로 메모
- `interim/`: 원본에서 1차 정제한 중간 산출물
- `processed/`: 학습용 최종 CSV
- `artifacts/`: 학습된 모델, tokenizer, metrics
- `configs/`: label map, training 설정
- `logs/`: 무인 실행 로그와 상태 파일
- `scripts/`: 데이터 준비, 학습, 평가, 추론, 무인 실행 스크립트

기본 학습 방향:

1. AI Hub 감성대화 `xlsx/json`을 학습용 `csv`로 가공
2. 서비스 라벨 6개 기준으로 매핑
3. `beomi/KcELECTRA-base`를 sequence classification으로 파인튜닝
4. `eval_macro_f1` 기준으로 best model 저장
5. FastAPI `/internal/model/emotion-classify`로 서빙

실행 순서:

1. `scripts/prepare_emotion_dataset.py`로 `processed/train_emotion_mvp.csv`, `processed/valid_emotion_mvp.csv` 생성
2. `scripts/train_emotion_classifier.py`로 1차 학습
3. `scripts/evaluate_emotion_classifier.py`로 검증
4. `scripts/infer_emotion_classifier.py`로 단건 추론 확인
5. FastAPI `app/routers/model_router.py`와 `app/services/emotion_classifier_service.py`로 내부 서빙 연결

무인 실행:

- `scripts/run_kcelectra_training.ps1`는 `prepare -> train -> evaluate -> verify api`를 한 번에 실행합니다.
- 로그는 `logs/<timestamp>/` 아래에 단계별로 쌓입니다.
- 상태 파일은 같은 폴더의 `status.json`에 기록됩니다.
- 빠르게 재실행하고 싶으면 `-SkipPrepare`, `-SkipTrain`, `-SkipEvaluate`, `-SkipVerifyApi` 옵션을 사용할 수 있습니다.

## CPU 기준 현실 설정

현재처럼 GPU 없이 CPU로만 학습할 때는 기본 설정 `configs/training_config.json`이 너무 무겁습니다.
그래서 다음 실행부터는 `configs/training_config_cpu.json`을 기본값으로 사용하도록 맞췄습니다.

핵심 차이:

- `max_length`: `128 -> 96`
- `num_train_epochs`: `5 -> 2`
- `train_batch_size`: `16 -> 4`
- `eval_batch_size`: `16 -> 4`
- `gradient_accumulation_steps`: `4`
- `early_stopping_patience`: `1`
- `save_total_limit`: `2 -> 1`

이렇게 줄인 이유:

- CPU에서는 시퀀스 길이와 배치 크기가 가장 큰 병목입니다.
- epoch를 줄이고 early stopping을 넣어야 너무 오래 도는 런을 줄일 수 있습니다.
- gradient accumulation으로 메모리 사용은 낮추고, 너무 작은 배치의 불안정성은 조금 보완할 수 있습니다.

## 라벨 매핑 보정 메모

2026-03-27 기준으로 `prepare_emotion_dataset.py`의 서비스 라벨 매핑을 보강했습니다.

- 기존 문제:
  - `emotion_minor`에 `노여워하는`, `혼란스러운`, `우울한` 같은 실제 데이터 표현이 많았는데 정확히 일치하는 키가 적어서 많은 행이 기본값 `CALM`으로 떨어졌습니다.
  - `emotion_major`의 `상처`, `당황`도 기본값 `CALM`으로 몰려 학습 분포가 왜곡됐습니다.
- 현재 보정:
  - exact match 라벨을 늘렸습니다.
  - `걱정`, `혼란`, `노여`, `우울` 같은 keyword match fallback을 추가했습니다.
  - major fallback을 `상처 -> SAD`, `당황 -> ANXIOUS`로 보정했습니다.
- 현재 주의:
  - 원본 처리 결과에는 아직 `TIRED` 샘플이 사실상 없어서 6라벨 중 `TIRED`는 학습 데이터 공백 상태입니다.
  - 따라서 현재 단계에서는 `TIRED` 예측 품질을 기대하면 안 되고, 별도 소스 확보나 매핑 전략 보완이 더 필요합니다.

기본 무인 실행:

```powershell
.\ai-api-fastapi\training\emotion_classifier\scripts\run_kcelectra_training.ps1
```

기존 기본 설정으로 강제로 실행:

```powershell
.\ai-api-fastapi\training\emotion_classifier\scripts\run_kcelectra_training.ps1 `
  -ConfigPath "C:\programing\mindcompass\ai-api-fastapi\training\emotion_classifier\configs\training_config.json"
```
