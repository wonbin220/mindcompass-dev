---
library_name: transformers
tags:
- korean
- emotion
- emotion-classification
- nlp
- electra
- koelectra
- sentiment
- sequence-classification
license: mit
datasets:
- LimYeri/kor-diary-emotion_v2
- qowlsdud/CounselGPT
language:
- ko
metrics:
- accuracy
- f1
base_model:
- monologg/koelectra-base-v3-discriminator
pipeline_tag: text-classification
---

# HowRU-KoELECTRA-Emotion-Classifier

## Model Description
KoELECTRA 기반의 한국어(특히 일기/심리 기록) 감정 분류 모델입니다.<br>
텍스트에서 8가지 감정(기쁨, 설렘, 평범함, 놀라움, 불쾌함, 두려움, 슬픔, 분노)을 인식합니다.

- **Model type:** Text Classification (Emotion Recognition)
- **Language:** Korean (한국어, ko)
- **License:** MIT
- **Finetuned from model:** [monologg/koelectra-base-v3-discriminator](https://huggingface.co/monologg/koelectra-base-v3-discriminator)

## Emotion Classes
이 모델은 입력된 한국어 문장의 주요 감정을 아래 8개 클래스 중 하나로 분류합니다.
| Emotion (Korean) | Emotion (EN) |
|------------------|--------------|
| 기쁨             | Joy          |
| 설렘             | Excitement   |
| 평범함           | Neutral      |
| 놀라움           | Surprise     |
| 불쾌함           | Disgust      |
| 두려움           | Fear         |
| 슬픔             | Sadness      |
| 분노             | Anger        |

---

## How to Get Started with the Model
```python
from transformers import AutoTokenizer, AutoModelForSequenceClassification
import torch
import torch.nn.functional as F

# 1) Load Model & Tokenizer
MODEL_NAME = "LimYeri/HowRU-KoELECTRA-Emotion-Classifier"

tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
model = AutoModelForSequenceClassification.from_pretrained(MODEL_NAME)

# GPU 사용 가능 시 자동 전환
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
model.to(device)
model.eval()

# 감정 라벨 매핑 (id2label)
id2label = model.config.id2label


# 2) Inference Function
def predict_emotion(text: str):
    """
    Returns:
        - top1_pred: 예측된 감정 라벨
        - probs_sorted: 감정별 확률(내림차순)
        - top2_pred: 상위 두 개의 감정
    """

    # 토크나이징
    inputs = tokenizer(
        text,
        return_tensors="pt",
        truncation=True,
        padding=True,
        max_length=512
    ).to(device)

    # 추론
    with torch.no_grad():
        logits = model(**inputs).logits
        probs = F.softmax(logits, dim=-1)[0]

    # 정렬된 확률
    probs_sorted = sorted(
        [(id2label[i], float(probs[i])) for i in range(len(probs))],
        key=lambda x: x[1],
        reverse=True
    )

    top1_pred = probs_sorted[0]
    top2_pred = probs_sorted[:2]

    return {
        "text": text,
        "top1_emotion": top1_pred,
        "top2_emotions": top2_pred,
        "all_probabilities": probs_sorted,
    }


# 3) Example
result = predict_emotion("오늘 정말 기분이 좋고 행복한 하루였어!")
print(result)
```

### pipeline
```python
from transformers import pipeline

MODEL_NAME = "LimYeri/HowRU-KoELECTRA-Emotion-Classifier"

classifier = pipeline(
    "text-classification",
    model=MODEL_NAME,
    tokenizer=MODEL_NAME,
    top_k=None   # 전체 감정 확률 반환
)

# 예측
text = "오늘 정말 기분이 좋고 행복한 하루였어!"
result = classifier(text)

result = result[0]

print("입력 문장:", text)
print("\nTop-1 감정:", result[0]['label'], f"({result[0]['score']:.4f})")
print("\n전체 감정 분포:")
for r in result:
    print(f"  {r['label']}: {r['score']:.4f}")
```

---

## Training Details

### Training Data
1. [LimYeri/kor-diary-emotion_v2](https://huggingface.co/datasets/LimYeri/kor-diary-emotion_v2)
2. [qowlsdud/CounselGPT](https://huggingface.co/datasets/qowlsdud/CounselGPT)

- **Total(8:2로 분할):** 50,000행
- **Train:** 40,000행
- **Validation:** 10,000행

### Training Procedure
- **Base Model**: [monologg/koelectra-base-v3-discriminator](https://huggingface.co/monologg/koelectra-base-v3-discriminator)
- **Objective**: Single-label classification
- **Max Length**: 512

### Training Hyperparameters
- **num_train_epochs**: 3
- **learning_rate**: 3e-5
- **weight_decay**: 0.02
- **warmup_ratio**: 0.15
- **per_device_train_batch_size**: 32
- **per_device_eval_batch_size**: 64
- **max_grad_norm**: 1.0

---

## Performance
| Metric          | Score  |
|-----------------|--------|
| **Eval Accuracy** | 0.95  |
| **Eval F1 Macro** | 0.95  |
| **Eval Loss**     | 0.16  |

---
## Model Architecture

### 1) ELECTRA Encoder (Base-size)
- **Hidden size:** 768
- **Layers:** 12 Transformer blocks
- **Attention heads:** 12
- **MLP intermediate size:** 3072
- **Activation:** GELU
- **Dropout:** 0.1

### 2) Classification Head
감정 8개 클래스를 예측하기 위한 추가 분류 헤드:

- **Dense Layer**: 768 → 768
- **Activation**: GELU
- **Dropout**: 0.1
- **Output Projection**: 768 → 8

---

## Citation
```bibtex
@misc{HowRUEmotion2025,
  title={HowRU KoELECTRA Emotion Classifier},
  author={Lim, Yeri},
  year={2025},
  publisher={Hugging Face},
  howpublished={\url{https://huggingface.co/LimYeri/HowRU-KoELECTRA-Emotion-Classifier}}
}
```