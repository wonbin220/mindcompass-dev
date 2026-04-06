# 파일: app/inference/stub_predictor.py
# 역할: 실제 모델 없이 고정 응답을 반환하는 stub predictor
# 호출: app/services/emotion_service.py -> StubPredictor

import hashlib

from app.inference.base_predictor import BasePredictor

# 지원하는 감정 레이블 목록
EMOTION_LABELS = [
    "joy",
    "sadness",
    "anger",
    "anxiety",
    "calm",
    "gratitude",
    "loneliness",
    "hope",
]

# 텍스트 키워드 -> 감정 매핑 (stub용)
KEYWORD_MAP = {
    "기쁘": "joy",
    "행복": "joy",
    "좋았": "joy",
    "슬프": "sadness",
    "우울": "sadness",
    "힘들": "sadness",
    "화가": "anger",
    "짜증": "anger",
    "불안": "anxiety",
    "걱정": "anxiety",
    "편안": "calm",
    "평화": "calm",
    "감사": "gratitude",
    "고마": "gratitude",
    "외로": "loneliness",
    "혼자": "loneliness",
    "희망": "hope",
    "기대": "hope",
}


class StubPredictor(BasePredictor):
    """개발/테스트용 stub predictor.

    실제 PyTorch 모델 없이 키워드 매칭으로 감정을 추정한다.
    키워드가 없으면 텍스트 해시 기반으로 결정론적 결과를 반환한다.

    이 predictor는 초기 개발 단계에서 엔드포인트 계약을 검증하기 위해 존재한다.
    실제 모델이 준비되면 BasePredictor를 구현한 새 predictor로 교체한다.
    """

    MODEL_NAME = "stub-keyword-classifier"
    MODEL_VERSION = "0.1.0-stub"
    THRESHOLD = 0.3

    def predict(self, text: str) -> dict:
        primary_emotion = self._detect_by_keyword(text)

        if primary_emotion is None:
            primary_emotion = self._detect_by_hash(text)

        scores = self._build_scores(primary_emotion)
        return {"scores": scores}

    def get_info(self) -> dict:
        return {
            "model_name": self.MODEL_NAME,
            "model_version": self.MODEL_VERSION,
            "status": "ready",
            "emotion_labels": EMOTION_LABELS,
            "threshold": self.THRESHOLD,
        }

    def _detect_by_keyword(self, text: str) -> str | None:
        """텍스트에서 키워드를 찾아 감정을 반환한다."""
        for keyword, emotion in KEYWORD_MAP.items():
            if keyword in text:
                return emotion
        return None

    def _detect_by_hash(self, text: str) -> str:
        """키워드가 없을 때 해시 기반으로 결정론적 감정을 반환한다."""
        digest = hashlib.md5(text.encode("utf-8")).hexdigest()
        index = int(digest[:8], 16) % len(EMOTION_LABELS)
        return EMOTION_LABELS[index]

    def _build_scores(self, primary: str) -> list[dict]:
        """primary 감정에 높은 점수를 주고, 나머지를 낮게 분배한다."""
        scores = []
        for label in EMOTION_LABELS:
            if label == primary:
                scores.append({"emotion": label, "score": 0.75})
            else:
                scores.append({"emotion": label, "score": 0.04})
        return scores
