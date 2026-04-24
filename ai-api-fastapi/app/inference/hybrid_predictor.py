# 파일: app/inference/hybrid_predictor.py
# 역할: tired_v5(TIRED 전담) + Seonghaa roberta(나머지 5개 감정) 앙상블
# 호출: app/services/emotion_service.py -> HybridPredictor

import torch
from transformers import AutoModelForSequenceClassification, AutoTokenizer, pipeline

from app.inference.base_predictor import BasePredictor

TIRED_MODEL_PATH = "training/emotion_classifier/artifacts/tired_v5/best"
LIMYERI_MODEL_PATH = "training/emotion_classifier/artifacts/limyeri"
TIRED_THRESHOLD = 0.6

EMOTION_LABELS = ["happy", "calm", "anxious", "sad", "angry", "tired"]

LIMYERI_LABEL_MAP = {
    "기쁨": "happy",
    "설렘": "happy",
    "평범함": "calm",
    "놀라움": "anxious",
    "불쾌함": "sad",
    "두려움": "anxious",
    "슬픔": "sad",
    "분노": "angry",
}
class HybridPredictor(BasePredictor):
    MODEL_NAME = "hybrid-tired-v5-seonghaa"
    MODEL_VERSION = "1.0.0"
    THRESHOLD = 0.3

    def __init__(self):
        self._device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        device_id = 0 if torch.cuda.is_available() else -1

        # tired_v5 로드
        self._tired_tokenizer = AutoTokenizer.from_pretrained(TIRED_MODEL_PATH)
        self._tired_model = AutoModelForSequenceClassification.from_pretrained(TIRED_MODEL_PATH)
        self._tired_model.to(self._device)
        self._tired_model.eval()

        # limyeri_model 로드
        self._seonghaa = pipeline(
            "text-classification",
            model=LIMYERI_MODEL_PATH,
            device=device_id,
            top_k=None,
        )

    def predict(self, text: str) -> dict:
        tired_score = self._get_tired_score(text)

        if tired_score >= TIRED_THRESHOLD:
            return self._build_tired_scores(text)

        return self._build_seonghaa_scores(text, tired_score)

    def _get_tired_score(self, text: str) -> float:
        inputs = self._tired_tokenizer(
            text, return_tensors="pt", max_length=512, truncation=True, padding=True
        ).to(self._device)
        with torch.no_grad():
            probs = torch.softmax(self._tired_model(**inputs).logits, dim=-1)[0]
        return float(probs[5])  # TIRED = index 5

    def _build_tired_scores(self, text: str) -> dict:
        inputs = self._tired_tokenizer(
            text, return_tensors="pt", max_length=512, truncation=True, padding=True
        ).to(self._device)
        with torch.no_grad():
            probs = torch.softmax(self._tired_model(**inputs).logits, dim=-1)[0]
        scores = [
            {"label": EMOTION_LABELS[i], "score": float(probs[i])}
            for i in range(len(probs))
        ]
        return {"scores": scores}

    def _build_seonghaa_scores(self, text: str, tired_score: float) -> dict:
        results = self._seonghaa(text, truncation=True, max_length=256)
        if results and isinstance(results[0], list):
            results = results[0]
        remaining = 1.0 - tired_score
        aggregated = {label: 0.0 for label in EMOTION_LABELS}
        aggregated["tired"] = tired_score
        for item in results:
            en_label = LIMYERI_LABEL_MAP.get(item["label"])
            if en_label:
                aggregated[en_label] += item["score"] * remaining
        scores = [{"label": k, "score": v} for k, v in aggregated.items()]
        return {"scores": scores}

    def get_info(self) -> dict:
        return {
            "model_name": self.MODEL_NAME,
            "model_version": self.MODEL_VERSION,
            "status": "ready",
            "emotion_labels": EMOTION_LABELS,
            "threshold": self.THRESHOLD,
        }
