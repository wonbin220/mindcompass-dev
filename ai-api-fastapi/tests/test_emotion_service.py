# 파일: tests/test_emotion_service.py
# 역할: EmotionService 단위 테스트 (mock predictor 주입)
# 호출: pytest tests/

import pytest
from unittest.mock import MagicMock

from app.services.emotion_service import EmotionService
from app.schemas.emotion import EmotionClassifyRequest


def make_predictor(scores=None, raise_error=False):
    """테스트용 mock predictor 팩토리."""
    predictor = MagicMock()
    predictor.get_info.return_value = {
        "model_name": "test-model",
        "model_version": "1.0.0",
        "status": "ready",
        "emotion_labels": ["happy", "calm", "anxious", "sad", "angry", "tired"],
        "threshold": 0.3,
    }
    if raise_error:
        predictor.predict.side_effect = RuntimeError("모델 추론 실패")
    else:
        predictor.predict.return_value = {
            "scores": scores or [
                {"label": "happy", "score": 0.7},
                {"label": "calm", "score": 0.2},
                {"label": "anxious", "score": 0.05},
                {"label": "sad", "score": 0.03},
                {"label": "angry", "score": 0.01},
                {"label": "tired", "score": 0.01},
            ]
        }
    return predictor


class TestEmotionServiceClassify:

    def test_정상_텍스트_primaryEmotion_반환(self):
        service = EmotionService(make_predictor())
        request = EmotionClassifyRequest(text="오늘 기분이 좋다")

        result = service.classify(request)

        assert result.primaryEmotion == "happy"
        assert result.fallbackUsed is False
        assert result.fallbackReason is None

    def test_빈_텍스트_fallback_반환(self):
        service = EmotionService(make_predictor())
        request = EmotionClassifyRequest(text="   ")

        result = service.classify(request)

        assert result.fallbackUsed is True
        assert result.fallbackReason == "EMPTY_TEXT"
        assert result.primaryEmotion == "neutral"

    def test_모델_에러_fallback_반환(self):
        service = EmotionService(make_predictor(raise_error=True))
        request = EmotionClassifyRequest(text="오늘 너무 힘들다")

        result = service.classify(request)

        assert result.fallbackUsed is True
        assert result.fallbackReason == "MODEL_ERROR"

    def test_scores_confidence_내림차순_정렬(self):
        service = EmotionService(make_predictor())
        request = EmotionClassifyRequest(text="오늘 기분이 좋다")

        result = service.classify(request)

        scores = [s.score for s in result.scores]
        assert scores == sorted(scores, reverse=True)
    def test_threshold_이상만_emotionTags_포함(self):
        # threshold=0.3, happy=0.7, calm=0.2 → happy만 태그
        service = EmotionService(make_predictor())
        request = EmotionClassifyRequest(text="오늘 기분이 좋다")

        result = service.classify(request)

        assert "happy" in result.emotionTags
        assert "calm" not in result.emotionTags  # 0.2 < 0.3

    def test_returnTopK_개수_제한(self):
        service = EmotionService(make_predictor())
        request = EmotionClassifyRequest(text="오늘 기분이 좋다", returnTopK=2)

        result = service.classify(request)

        assert len(result.scores) == 2
        
class TestEmotionServiceRuntimeInfo:

    def test_runtime_info_반환(self):
        service = EmotionService(make_predictor())

        result = service.get_runtime_info()

        assert result.modelName == "test-model"
        assert result.status == "ready"
        assert len(result.emotionLabels) == 6
