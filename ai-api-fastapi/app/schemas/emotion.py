# 파일: app/schemas/emotion.py
# 역할: 감정분류 요청/응답 스키마 정의
# 호출: app/routers/model.py, app/services/emotion_service.py

from pydantic import BaseModel, Field


class EmotionClassifyRequest(BaseModel):
    """감정분류 요청 스키마.

    ai-api가 이 형식으로 요청을 보낸다.
    text만 필수이고, sessionId는 로깅/추적용 선택 필드다.
    """

    text: str = Field(..., description="감정분류 대상 텍스트")
    returnTopK: int | None = Field(
        default=3, description="반환할 상위 감정 개수 (선택)"
    )
    sessionId: str | None = Field(
        default=None, description="추적용 세션 ID (선택)"
    )


class EmotionScore(BaseModel):
    """개별 감정의 확률 점수."""

    label: str = Field(..., description="감정 레이블 (예: joy, sadness)")
    score: float = Field(..., ge=0.0, le=1.0, description="확률 점수 (0~1)")


class EmotionClassifyResponse(BaseModel):
    """감정분류 응답 스키마.

    ai-api가 이 응답을 받아서 후속 처리(위험도 계산, 답변 생성 등)를 수행한다.
    fallback 관련 필드는 모델 실패 시에도 구조화된 응답을 보장하기 위해 포함한다.
    """

    primaryEmotion: str = Field(
        ..., description="가장 높은 확률의 감정 레이블"
    )
    confidence: float = Field(
        ..., ge=0.0, le=1.0, description="primaryEmotion의 확률"
    )
    emotionTags: list[str] = Field(
        default_factory=list,
        description="threshold를 넘은 감정 레이블 목록",
    )
    scores: list[EmotionScore] = Field(
        default_factory=list,
        description="전체 감정별 확률 점수 목록",
    )
    modelName: str = Field(..., description="추론에 사용된 모델 이름")
    modelVersion: str = Field(..., description="추론에 사용된 모델 버전")
    fallbackUsed: bool = Field(
        default=False, description="fallback 응답 여부"
    )
    fallbackReason: str | None = Field(
        default=None, description="fallback 사유 (fallbackUsed=true일 때)"
    )


class RuntimeInfoResponse(BaseModel):
    """모델 런타임 정보 응답 스키마."""

    modelName: str = Field(..., description="현재 로드된 모델 이름")
    modelVersion: str = Field(..., description="모델 버전")
    status: str = Field(..., description="모델 상태 (ready / loading / error)")
    emotionLabels: list[str] = Field(
        default_factory=list, description="지원하는 감정 레이블 목록"
    )
    threshold: float = Field(
        ..., description="emotionTags 판정에 사용하는 확률 임계값"
    )
