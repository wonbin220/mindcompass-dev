# 파일: tests/test_model_router.py
# 역할: /internal/model/* 엔드포인트 통합 테스트
# 호출: pytest tests/

from unittest.mock import MagicMock
from fastapi.testclient import TestClient

from app.main import app
from app.services.emotion_service import get_emotion_service
from app.schemas.emotion import EmotionClassifyResponse, RuntimeInfoResponse


def make_mock_service(fallback=False):
    """테스트용 mock EmotionService."""
    service = MagicMock()
    service.get_runtime_info.return_value = RuntimeInfoResponse(
        modelName="test-model",
        modelVersion="1.0.0",
        status="ready",
        emotionLabels=["happy", "calm", "anxious", "sad", "angry", "tired"],
        threshold=0.3,
    )
    service.classify.return_value = EmotionClassifyResponse(
        primaryEmotion="neutral" if fallback else "happy",
        confidence=0.0 if fallback else 0.7,
        emotionTags=[] if fallback else ["happy"],
        scores=[],
        modelName="test-model",
        modelVersion="1.0.0",
        fallbackUsed=fallback,
        fallbackReason="EMPTY_TEXT" if fallback else None,
    )
    return service

class TestModelRouter:

    def setup_method(self):
        """각 테스트 전 dependency override 초기화."""
        app.dependency_overrides.clear()

    def _client(self, fallback=False):
        mock_service = make_mock_service(fallback=fallback)
        app.dependency_overrides[get_emotion_service] = lambda: mock_service
        return TestClient(app)

    def test_runtime_info_200(self):
        client = self._client()
        response = client.get("/internal/model/runtime-info")

        assert response.status_code == 200
        body = response.json()
        assert body["modelName"] == "test-model"
        assert body["status"] == "ready"

    def test_emotion_classify_200(self):
        client = self._client()
        response = client.post(
            "/internal/model/emotion-classify",
            json={"text": "오늘 기분이 좋다"},
        )

        assert response.status_code == 200
        body = response.json()
        assert body["primaryEmotion"] == "happy"
        assert body["fallbackUsed"] is False

    def test_emotion_classify_fallback_응답_구조(self):
        client = self._client(fallback=True)
        response = client.post(
            "/internal/model/emotion-classify",
            json={"text": "   "},
        )

        assert response.status_code == 200
        body = response.json()
        assert body["fallbackUsed"] is True
        assert body["fallbackReason"] == "EMPTY_TEXT"

    def test_emotion_classify_text_없으면_422(self):
        client = self._client()
        response = client.post(
            "/internal/model/emotion-classify",
            json={},  # text 필드 누락
        )

        assert response.status_code == 422
