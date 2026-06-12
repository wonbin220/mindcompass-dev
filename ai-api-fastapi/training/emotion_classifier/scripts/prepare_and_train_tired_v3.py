# 파일: prepare_and_train_tired_v3.py
# 역할: TIRED 데이터 보강 후 6-class 감정 분류 모델 학습
# 실행: python scripts/prepare_and_train_tired_v3.py

import json
import os
import random
import pandas as pd
import numpy as np
from pathlib import Path
from collections import Counter
from sklearn.model_selection import train_test_split

import torch
from torch.utils.data import Dataset
from transformers import (
    AutoTokenizer,
    AutoModelForSequenceClassification,
    TrainingArguments,
    Trainer,
    EarlyStoppingCallback,
)
from sklearn.metrics import classification_report, f1_score, confusion_matrix

# ── 경로 설정 ──────────────────────────────────────────────────────────────────
BASE = Path(__file__).resolve().parents[1]
TRAIN_JSON = Path("/home/wonbin220/mindcompass-dev/csv파일들/Training_221115_add/라벨링데이터/감성대화말뭉치(최종데이터)_Training.json")
VALID_JSON = Path("/home/wonbin220/mindcompass-dev/csv파일들/Validation_221115_add/라벨링데이터/감성대화말뭉치(최종데이터)_Validation.json")
TIRED_EXTRA_CSV = Path("/home/wonbin220/mindcompass-dev/csv파일들/tired_extracted_ko.csv")
PROCESSED_DIR = BASE / "processed"
ARTIFACTS_DIR = BASE / "artifacts" / "tired_v3"
PROCESSED_DIR.mkdir(parents=True, exist_ok=True)
ARTIFACTS_DIR.mkdir(parents=True, exist_ok=True)

# ── 설정 ──────────────────────────────────────────────────────────────────────
MODEL_NAME = "beomi/KcELECTRA-base"
MAX_LENGTH = 96
SEED = 42
TRAIN_BATCH = 8
EVAL_BATCH = 8
GRAD_ACCUM = 2
NUM_EPOCHS = 3
LR = 2e-5

random.seed(SEED)
np.random.seed(SEED)
torch.manual_seed(SEED)

# ── 라벨 매핑 ─────────────────────────────────────────────────────────────────
LABEL2ID = {"HAPPY": 0, "CALM": 1, "ANXIOUS": 2, "SAD": 3, "ANGRY": 4, "TIRED": 5}
ID2LABEL = {v: k for k, v in LABEL2ID.items()}

# 감정 코드 -> 서비스 라벨 매핑
def code_to_label(code: str) -> str:
    n = int(code[1:])  # E10 -> 10
    if 10 <= n <= 18:
        return "HAPPY"
    elif 19 <= n <= 27:
        return "ANXIOUS"
    elif 28 <= n <= 35:
        return "ANXIOUS"   # 당황
    elif n == 36:
        return None         # 키워드 필터로 별도 처리
    elif 37 <= n <= 45:
        return "SAD"
    elif 46 <= n <= 54:
        return "ANGRY"
    elif 55 <= n <= 63:
        return "SAD"        # 상처 -> SAD
    elif 64 <= n <= 69:
        return "CALM"       # 중립
    return None

TIRED_KEYWORDS = [
    "기운이 빠", "힘에 부쳐", "피곤", "지쳐", "무기력", "탈진", "번아웃",
    "지친", "녹초", "기운이 없", "힘이 없", "에너지가 없", "너무 힘들", "지쳐버",
    "지쳐있", "완전히 지", "몸이 무거", "일어나기 힘", "아무것도 하기 싫",
]

def has_tired_keyword(text: str) -> bool:
    return any(kw in text for kw in TIRED_KEYWORDS)

# ── 텍스트 추출 ────────────────────────────────────────────────────────────────
def extract_texts(data: list, include_e36: bool = False):
    rows = []
    for item in data:
        code = item["profile"]["emotion"]["type"]
        content = item["talk"]["content"]

        label = code_to_label(code)

        # E36: 키워드 필터
        if code == "E36":
            for k in ["HS01", "HS02", "HS03"]:
                text = content.get(k, "").strip()
                if text and has_tired_keyword(text):
                    rows.append({"text": text, "label": "TIRED"})
            continue

        if label is None:
            continue

        for k in ["HS01", "HS02", "HS03"]:
            text = content.get(k, "").strip()
            if text:
                rows.append({"text": text, "label": label})

    return rows

# ── 데이터 로드 ────────────────────────────────────────────────────────────────
print("Loading training JSON...")
with open(TRAIN_JSON, "r", encoding="utf-8") as f:
    train_data = json.load(f)

print("Loading validation JSON...")
with open(VALID_JSON, "r", encoding="utf-8") as f:
    valid_data = json.load(f)

print("Extracting texts...")
train_rows = extract_texts(train_data)
valid_rows = extract_texts(valid_data)

# TIRED 추가 데이터 (tired_extracted_ko.csv)
tired_extra = pd.read_csv(TIRED_EXTRA_CSV)
for _, row in tired_extra.iterrows():
    text = str(row["text_ko"]).strip()
    if text:
        train_rows.append({"text": text, "label": "TIRED"})

train_df = pd.DataFrame(train_rows).drop_duplicates(subset=["text"])
valid_df = pd.DataFrame(valid_rows).drop_duplicates(subset=["text"])

# ── TIRED 검증용 분리 + 다수 클래스 캡핑 ──────────────────────────────────────
MAX_PER_CLASS = 5000  # 다수 클래스 최대 샘플 수 (TIRED는 제외)

# TIRED: 20%를 검증으로 이동
tired_train = train_df[train_df["label"] == "TIRED"]
if len(tired_train) > 10:
    tired_val_split, tired_keep = train_test_split(tired_train, test_size=0.8, random_state=SEED)
else:
    tired_val_split, tired_keep = pd.DataFrame(), tired_train
valid_df = pd.concat([valid_df, tired_val_split]).reset_index(drop=True)

# 다수 클래스 캡핑
capped_parts = [tired_keep]
for label in ["HAPPY", "CALM", "ANXIOUS", "SAD", "ANGRY"]:
    part = train_df[train_df["label"] == label]
    if len(part) > MAX_PER_CLASS:
        part = part.sample(n=MAX_PER_CLASS, random_state=SEED)
    capped_parts.append(part)
train_df = pd.concat(capped_parts).reset_index(drop=True)

print("\n=== 학습 데이터 분포 (캡핑 후) ===")
print(Counter(train_df["label"]))
print("\n=== 검증 데이터 분포 ===")
print(Counter(valid_df["label"]))

train_df.to_csv(PROCESSED_DIR / "train_tired_v3.csv", index=False)
valid_df.to_csv(PROCESSED_DIR / "valid_tired_v3.csv", index=False)
print(f"\nCSV 저장 완료: {PROCESSED_DIR}")

# ── 클래스 가중치 계산 ──────────────────────────────────────────────────────────
label_counts = Counter(train_df["label"])
total = sum(label_counts.values())
num_classes = len(LABEL2ID)
class_weights = torch.zeros(num_classes)
for label, idx in LABEL2ID.items():
    count = label_counts.get(label, 1)
    class_weights[idx] = total / (num_classes * count)

print("\n=== 클래스 가중치 ===")
for label, idx in LABEL2ID.items():
    print(f"  {label}: {class_weights[idx]:.3f}")

# ── Dataset ───────────────────────────────────────────────────────────────────
tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)

class EmotionDataset(Dataset):
    def __init__(self, df: pd.DataFrame):
        self.texts = df["text"].tolist()
        self.labels = [LABEL2ID[l] for l in df["label"]]

    def __len__(self):
        return len(self.texts)

    def __getitem__(self, idx):
        enc = tokenizer(
            self.texts[idx],
            max_length=MAX_LENGTH,
            truncation=True,
            padding="max_length",
            return_tensors="pt",
        )
        return {
            "input_ids": enc["input_ids"].squeeze(),
            "attention_mask": enc["attention_mask"].squeeze(),
            "labels": torch.tensor(self.labels[idx], dtype=torch.long),
        }

train_dataset = EmotionDataset(train_df)
valid_dataset = EmotionDataset(valid_df)

# ── 가중치 손실 Trainer ────────────────────────────────────────────────────────
class WeightedTrainer(Trainer):
    def __init__(self, class_weights, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.class_weights = class_weights

    def compute_loss(self, model, inputs, return_outputs=False, **kwargs):
        labels = inputs.pop("labels")
        outputs = model(**inputs)
        logits = outputs.logits
        weights = self.class_weights.to(logits.device)
        loss_fn = torch.nn.CrossEntropyLoss(weight=weights)
        loss = loss_fn(logits, labels)
        return (loss, outputs) if return_outputs else loss

# ── 평가 지표 ──────────────────────────────────────────────────────────────────
def compute_metrics(eval_pred):
    logits, labels = eval_pred
    preds = np.argmax(logits, axis=-1)
    macro_f1 = f1_score(labels, preds, average="macro", zero_division=0)
    tired_f1 = f1_score(labels, preds, labels=[LABEL2ID["TIRED"]], average="macro", zero_division=0)
    return {"eval_macro_f1": macro_f1, "eval_tired_f1": tired_f1}

# ── 모델 로드 ──────────────────────────────────────────────────────────────────
print("\nLoading model...")
model = AutoModelForSequenceClassification.from_pretrained(
    MODEL_NAME,
    num_labels=num_classes,
    id2label=ID2LABEL,
    label2id=LABEL2ID,
)

# ── 학습 설정 ──────────────────────────────────────────────────────────────────
training_args = TrainingArguments(
    output_dir=str(ARTIFACTS_DIR),
    num_train_epochs=NUM_EPOCHS,
    per_device_train_batch_size=TRAIN_BATCH,
    per_device_eval_batch_size=EVAL_BATCH,
    gradient_accumulation_steps=GRAD_ACCUM,
    learning_rate=LR,
    weight_decay=0.01,
    warmup_ratio=0.05,
    eval_strategy="epoch",
    save_strategy="epoch",
    load_best_model_at_end=True,
    metric_for_best_model="eval_macro_f1",
    greater_is_better=True,
    save_total_limit=1,
    seed=SEED,
    logging_steps=100,
    dataloader_num_workers=0,
    fp16=False,
)

trainer = WeightedTrainer(
    class_weights=class_weights,
    model=model,
    args=training_args,
    train_dataset=train_dataset,
    eval_dataset=valid_dataset,
    compute_metrics=compute_metrics,
    callbacks=[EarlyStoppingCallback(early_stopping_patience=2)],
)

print("\n=== 학습 시작 ===")
trainer.train()

# ── best model 저장 ────────────────────────────────────────────────────────────
best_dir = ARTIFACTS_DIR / "best"
trainer.save_model(str(best_dir))
tokenizer.save_pretrained(str(best_dir))
print(f"\nbest model 저장: {best_dir}")

# ── 최종 평가 ──────────────────────────────────────────────────────────────────
print("\n=== 최종 검증 ===")
preds_output = trainer.predict(valid_dataset)
preds = np.argmax(preds_output.predictions, axis=-1)
true_labels = [LABEL2ID[l] for l in valid_df["label"]]
target_names = [ID2LABEL[i] for i in range(num_classes)]
report = classification_report(true_labels, preds, target_names=target_names, zero_division=0)
print(report)

cm = confusion_matrix(true_labels, preds)
print("Confusion Matrix:")
print(cm)

# 결과 저장
eval_result = {
    "classification_report": classification_report(
        true_labels, preds, target_names=target_names, zero_division=0, output_dict=True
    ),
    "confusion_matrix": cm.tolist(),
}
eval_path = BASE / "artifacts" / "evaluation" / "valid_metrics_tired_v3.json"
eval_path.parent.mkdir(parents=True, exist_ok=True)
with open(eval_path, "w") as f:
    json.dump(eval_result, f, indent=2, ensure_ascii=False)
print(f"\n평가 결과 저장: {eval_path}")
