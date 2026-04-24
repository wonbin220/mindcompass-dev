# 파일: app/inference/kcelectra_predictor.py
# 역할: KcELECTRA 기반 실제 감정분류 predictor
# 호출: app/services/emotion_service.py -> KcElectraPredictor

import torch
from transformers import AutoModelForSequenceClassification, AutoTokenizer

from app.inference.base_predictor import BasePredictor

# uvicorn을 ai-api-fastapi/ 루트에서 실행한다고 가정
MODEL_PATH = "training/emotion_classifier/artifacts/tired_v5/best"

EMOTION_LABELS = ["happy", "calm", "anxious", "sad", "angry", "tired"]


class KcElectraPredictor(BasePredictor):
    """KcELECTRA 기반 감정분류 predictor.

    tired_v5 모델을 로드해 6가지 감정(HAPPY/CALM/ANXIOUS/SAD/ANGRY/TIRED)을
    분류한다. GPU가 없으면 자동으로 CPU로 폴백한다.
    """

    MODEL_NAME = "kcelectra-tired-v5"
    MODEL_VERSION = "1.0.0"
    THRESHOLD = 0.3

    def __init__(self, model_path: str = MODEL_PATH):
        self._device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self._tokenizer = AutoTokenizer.from_pretrained(model_path)
        self._model = AutoModelForSequenceClassification.from_pretrained(model_path)
        self._model.to(self._device)
        self._model.eval()

    def predict(self, text: str) -> dict:
        inputs = self._tokenizer(
            text,
            return_tensors="pt",
            max_length=512,
            truncation=True,
            padding=True,
        ).to(self._device)

        with torch.no_grad():
            logits = self._model(**inputs).logits
            probs = torch.softmax(logits, dim=-1)[0]
        scores = [
            {"label": EMOTION_LABELS[i], "score": float(probs[i])}
            for i in range(len(probs))
        ]
        return {"scores": scores}

    def get_info(self) -> dict:
        return {
            "model_name": self.MODEL_NAME,
            "model_version": self.MODEL_VERSION,
            "status": "ready",
            "emotion_labels": EMOTION_LABELS,
            "threshold": self.THRESHOLD,
        }
