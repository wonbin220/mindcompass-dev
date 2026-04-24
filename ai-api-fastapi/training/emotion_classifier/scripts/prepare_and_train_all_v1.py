# 파일: prepare_and_train_all_v1.py
# 역할: 6개 감정을 균등 규칙으로 전처리하고 감정 분류 모델을 학습
# 실행: python scripts/prepare_and_train_all_v1.py

import json
import random
from collections import Counter
from pathlib import Path

import numpy as np
import pandas as pd
import torch
from sklearn.metrics import classification_report, confusion_matrix, f1_score
from torch.utils.data import Dataset
from transformers import (
    AutoModelForSequenceClassification,
    AutoTokenizer,
    EarlyStoppingCallback,
    Trainer,
    TrainingArguments,
)

# ── 경로 설정 ──────────────────────────────────────────────────────────────────
BASE = Path(__file__).resolve().parents[1]
TRAIN_JSON = Path("/home/wonbin220/mindcompass-dev/csv파일들/Training_221115_add/라벨링데이터/감성대화말뭉치(최종데이터)_Training.json")
VALID_JSON = Path("/home/wonbin220/mindcompass-dev/csv파일들/Validation_221115_add/라벨링데이터/감성대화말뭉치(최종데이터)_Validation.json")
TIRED_EXTRA_CSV = Path("/home/wonbin220/mindcompass-dev/csv파일들/tired_extracted_ko.csv")
PROCESSED_DIR = BASE / "processed"
ARTIFACTS_DIR = BASE / "artifacts" / "all_v1"
BEST_DIR = ARTIFACTS_DIR / "best"
EVAL_PATH = BASE / "artifacts" / "evaluation" / "valid_metrics_all_v1.json"

PROCESSED_DIR.mkdir(parents=True, exist_ok=True)
ARTIFACTS_DIR.mkdir(parents=True, exist_ok=True)
BEST_DIR.mkdir(parents=True, exist_ok=True)
EVAL_PATH.parent.mkdir(parents=True, exist_ok=True)

# ── 설정 ──────────────────────────────────────────────────────────────────────
MODEL_NAME = "beomi/KcELECTRA-base"
MAX_LENGTH = 96
SEED = 42
TRAIN_BATCH = 8
EVAL_BATCH = 8
GRAD_ACCUM = 2
NUM_EPOCHS = 5
LR = 2e-5
CAP_PER_CLASS = 10_000

random.seed(SEED)
np.random.seed(SEED)
torch.manual_seed(SEED)

# ── 라벨 매핑 ─────────────────────────────────────────────────────────────────
LABEL2ID = {"HAPPY": 0, "CALM": 1, "ANXIOUS": 2, "SAD": 3, "ANGRY": 4, "TIRED": 5}
ID2LABEL = {v: k for k, v in LABEL2ID.items()}


def code_to_label(code: str) -> str | None:
    """감정 코드 범위를 서비스 라벨로 변환한다."""
    n = int(code[1:])
    if 10 <= n <= 18:
        return "HAPPY"
    if 19 <= n <= 35:
        return "ANXIOUS"
    if n == 36:
        return None
    if 37 <= n <= 45:
        return "SAD"
    if 46 <= n <= 54:
        return "ANGRY"
    if 55 <= n <= 63:
        return "SAD"
    if 64 <= n <= 69:
        return "CALM"
    return None


TIRED_KEYWORDS = [
    "기운이 빠",
    "힘에 부쳐",
    "피곤",
    "지쳐",
    "무기력",
    "탈진",
    "번아웃",
    "지친",
    "녹초",
    "기운이 없",
    "힘이 없",
    "에너지가 없",
    "너무 힘들",
    "지쳐버",
    "지쳐있",
    "완전히 지",
    "몸이 무거",
    "일어나기 힘",
    "아무것도 하기 싫",
]


def has_tired_keyword(text: str) -> bool:
    return any(keyword in text for keyword in TIRED_KEYWORDS)


def extract_texts(data: list) -> list[dict[str, str]]:
    """원본 JSON에서 text/label 행을 추출한다."""
    rows = []
    for item in data:
        code = item["profile"]["emotion"]["type"]
        content = item["talk"]["content"]
        label = code_to_label(code)

        if code == "E36":
            for key in ["HS01", "HS02", "HS03"]:
                text = content.get(key, "").strip()
                if text and has_tired_keyword(text):
                    rows.append({"text": text, "label": "TIRED"})
            continue

        if label is None:
            continue

        for key in ["HS01", "HS02", "HS03"]:
            text = content.get(key, "").strip()
            if text:
                rows.append({"text": text, "label": label})

    return rows


def cap_dataframe_per_class(df: pd.DataFrame, cap: int) -> pd.DataFrame:
    """각 클래스를 동일한 상한으로 샘플링한다."""
    capped_parts = []
    for label in LABEL2ID:
        part = df[df["label"] == label]
        if len(part) > cap:
            part = part.sample(n=cap, random_state=SEED)
        capped_parts.append(part)
    return pd.concat(capped_parts, ignore_index=True)


print("Loading training JSON...")
with open(TRAIN_JSON, "r", encoding="utf-8") as f:
    train_data = json.load(f)

print("Loading validation JSON...")
with open(VALID_JSON, "r", encoding="utf-8") as f:
    valid_data = json.load(f)

print("Extracting texts...")
train_rows = extract_texts(train_data)
valid_rows = extract_texts(valid_data)

# TIRED 보강 데이터는 학습 데이터에만 추가한다.
tired_extra = pd.read_csv(TIRED_EXTRA_CSV)
for _, row in tired_extra.iterrows():
    text = str(row["text_ko"]).strip()
    if text:
        train_rows.append({"text": text, "label": "TIRED"})

# 경계 문장 데이터가 있으면 학습 데이터에만 병합한다.
boundary_csv = PROCESSED_DIR / "korean_emotion_boundary_sentences.csv"
if boundary_csv.exists():
    boundary_df = pd.read_csv(boundary_csv)
    boundary_rows = [
        {"text": str(row["text"]).strip(), "label": str(row["label"]).strip()}
        for _, row in boundary_df.iterrows()
        if str(row["text"]).strip() and str(row["label"]).strip() in LABEL2ID
    ]
    train_rows.extend(boundary_rows)

train_df = pd.DataFrame(train_rows).drop_duplicates(subset=["text"]).reset_index(drop=True)
valid_df = pd.DataFrame(valid_rows).drop_duplicates(subset=["text"]).reset_index(drop=True)

train_df = cap_dataframe_per_class(train_df, CAP_PER_CLASS)
valid_df = cap_dataframe_per_class(valid_df, CAP_PER_CLASS)

print("\n=== 학습 데이터 분포 (캡핑 후) ===")
print(Counter(train_df["label"]))
print("\n=== 검증 데이터 분포 (캡핑 후) ===")
print(Counter(valid_df["label"]))

train_csv_path = PROCESSED_DIR / "train_all_v1.csv"
valid_csv_path = PROCESSED_DIR / "valid_all_v1.csv"
train_df.to_csv(train_csv_path, index=False)
valid_df.to_csv(valid_csv_path, index=False)
print(f"\nCSV 저장 완료: {train_csv_path}")
print(f"CSV 저장 완료: {valid_csv_path}")

# 균등 학습 규칙: 클래스 가중치는 모두 동일하게 둔다.
num_classes = len(LABEL2ID)
class_weights = torch.ones(num_classes, dtype=torch.float)

print("\n=== 클래스 가중치 ===")
for label, idx in LABEL2ID.items():
    print(f"  {label}: {class_weights[idx]:.3f}")

tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)


class EmotionDataset(Dataset):
    def __init__(self, df: pd.DataFrame):
        self.texts = df["text"].tolist()
        self.labels = [LABEL2ID[label] for label in df["label"]]

    def __len__(self):
        return len(self.texts)

    def __getitem__(self, idx):
        encoded = tokenizer(
            self.texts[idx],
            max_length=MAX_LENGTH,
            truncation=True,
            padding="max_length",
            return_tensors="pt",
        )
        return {
            "input_ids": encoded["input_ids"].squeeze(),
            "attention_mask": encoded["attention_mask"].squeeze(),
            "labels": torch.tensor(self.labels[idx], dtype=torch.long),
        }


train_dataset = EmotionDataset(train_df)
valid_dataset = EmotionDataset(valid_df)


class WeightedTrainer(Trainer):
    def __init__(self, class_weights, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.class_weights = class_weights

    def compute_loss(self, model, inputs, return_outputs=False, **kwargs):
        labels = inputs.pop("labels")
        outputs = model(**inputs)
        logits = outputs.logits
        loss_fn = torch.nn.CrossEntropyLoss(weight=self.class_weights.to(logits.device))
        loss = loss_fn(logits, labels)
        return (loss, outputs) if return_outputs else loss


def compute_metrics(eval_pred):
    logits, labels = eval_pred
    preds = np.argmax(logits, axis=-1)
    macro_f1 = f1_score(labels, preds, average="macro", zero_division=0)
    weighted_f1 = f1_score(labels, preds, average="weighted", zero_division=0)
    return {"macro_f1": macro_f1, "weighted_f1": weighted_f1}


print("\nLoading model...")
model = AutoModelForSequenceClassification.from_pretrained(
    MODEL_NAME,
    num_labels=num_classes,
    id2label=ID2LABEL,
    label2id=LABEL2ID,
)

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
    metric_for_best_model="macro_f1",
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

trainer.save_model(str(BEST_DIR))
tokenizer.save_pretrained(str(BEST_DIR))
print(f"\nbest model 저장: {BEST_DIR}")

print("\n=== 최종 검증 ===")
preds_output = trainer.predict(valid_dataset)
preds = np.argmax(preds_output.predictions, axis=-1)
true_labels = [LABEL2ID[label] for label in valid_df["label"]]
target_names = [ID2LABEL[i] for i in range(num_classes)]

report_text = classification_report(true_labels, preds, target_names=target_names, zero_division=0)
print(report_text)

cm = confusion_matrix(true_labels, preds)
print("Confusion Matrix:")
print(cm)

eval_result = {
    "guard_metric": "macro_f1",
    "classification_report": classification_report(
        true_labels,
        preds,
        target_names=target_names,
        zero_division=0,
        output_dict=True,
    ),
    "confusion_matrix": cm.tolist(),
}

with open(EVAL_PATH, "w", encoding="utf-8") as f:
    json.dump(eval_result, f, indent=2, ensure_ascii=False)

print(f"\n평가 결과 저장: {EVAL_PATH}")
