# 파일: app/services/emotion_service.py
# 역할: 감정분류 요청 처리 서비스 (입력 검증, predictor 호출, fallback 처리)
# 호출: app/routers/model.py -> EmotionService

import logging

from app.inference.base_predictor import BasePredictor
from app.inference.hybrid_predictor import HybridPredictor
from app.schemas.emotion import (
    EmotionClassifyRequest,
    EmotionClassifyResponse,
    EmotionScore,
    RuntimeInfoResponse,
)

logger = logging.getLogger(__name__)

# 감정분류 서비스 싱글턴
_emotion_service: "EmotionService | None" = None


def get_emotion_service() -> "EmotionService":
    """FastAPI Depends용 팩토리 함수.

    앱 전체에서 하나의 EmotionService 인스턴스를 공유한다.
    나중에 실제 모델로 교체할 때 이 함수에서 predictor만 바꾸면 된다.
    """
    global _emotion_service
    if _emotion_service is None:
        predictor = HybridPredictor()
        _emotion_service = EmotionService(predictor)
    return _emotion_service


class EmotionService:
    """감정분류 서비스.

    predictor를 주입받아 추론을 수행한다.
    입력 검증과 fallback 처리는 이 서비스가 담당한다.
    """

    def __init__(self, predictor: BasePredictor):
        self._predictor = predictor

    def get_runtime_info(self) -> RuntimeInfoResponse:
        info = self._predictor.get_info()
        return RuntimeInfoResponse(
            modelName=info["model_name"],
            modelVersion=info["model_version"],
            status=info["status"],
            emotionLabels=info["emotion_labels"],
            threshold=info["threshold"],
        )

    def classify(self, request: EmotionClassifyRequest) -> EmotionClassifyResponse:
        info = self._predictor.get_info()

        # --- empty text fallback ---
        if not request.text or not request.text.strip():
            logger.warning("빈 텍스트 입력 -> fallback 응답 반환")
            return self._fallback_response(
                info, reason="EMPTY_TEXT"
            )

        # --- model inference ---
        try:
            result = self._predictor.predict(request.text)
        except Exception as e:
            logger.error("모델 추론 실패 -> fallback 응답 반환: %s", e)
            return self._fallback_response(
                info, reason="MODEL_ERROR"
            )

        # scores를 confidence 내림차순으로 정렬
        sorted_scores = sorted(
            result["scores"], key=lambda s: s["score"], reverse=True
        )

        primary = sorted_scores[0]
        
        # NOTE: 이전 TIRED fallback 로직은 감정 카테고리 변경으로 삭제됨

        threshold = info["threshold"]
        emotion_tags = [s["label"] for s in sorted_scores if s["score"] >= threshold]

        return EmotionClassifyResponse(
            primaryEmotion=primary["label"],
            confidence=primary["score"],
            emotionTags=emotion_tags,
            scores=[
                EmotionScore(label=s["label"], score=s["score"])
                for s in sorted_scores[:request.returnTopK]
            ],
            modelName=info["model_name"],
            modelVersion=info["model_version"],
            fallbackUsed=False,
            fallbackReason=None,
        )

    def _fallback_response(
        self, info: dict, reason: str
    ) -> EmotionClassifyResponse:
        """fallback 응답 생성.

        모델 실패나 빈 입력 시에도 구조화된 응답을 반환한다.
        ai-api는 이 응답의 fallbackUsed 필드를 보고 후속 처리를 결정한다.
        """
        return EmotionClassifyResponse(
            primaryEmotion="neutral",
            confidence=0.0,
            emotionTags=[],
            scores=[],
            modelName=info["model_name"],
            modelVersion=info["model_version"],
            fallbackUsed=True,
            fallbackReason=reason,
        )
