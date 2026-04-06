# 파일: app/inference/base_predictor.py
# 역할: 감정분류 predictor 인터페이스 정의
# 호출: StubPredictor, 향후 실제 모델 predictor가 이 인터페이스를 구현

from abc import ABC, abstractmethod


class BasePredictor(ABC):
    """감정분류 predictor 인터페이스.

    모든 predictor는 이 클래스를 상속해야 한다.
    새 모델을 추가할 때 이 인터페이스를 구현하고,
    emotion_service.py의 get_emotion_service()에서 교체하면 된다.
    """

    @abstractmethod
    def predict(self, text: str) -> dict:
        """텍스트를 받아 감정분류 결과를 반환한다.

        반환 형식:
            {
                "scores": [
                    {"emotion": "joy", "score": 0.85},
                    {"emotion": "sadness", "score": 0.10},
                    ...
                ]
            }
        """
        ...

    @abstractmethod
    def get_info(self) -> dict:
        """현재 모델의 런타임 정보를 반환한다.

        반환 형식:
            {
                "model_name": "stub-classifier",
                "model_version": "0.1.0",
                "status": "ready",
                "emotion_labels": ["joy", "sadness", ...],
                "threshold": 0.3,
            }
        """
        ...
