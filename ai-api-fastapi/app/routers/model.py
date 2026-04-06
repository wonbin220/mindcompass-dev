# 파일: app/routers/model.py
# 역할: 감정분류 모델 관련 엔드포인트
# 호출: ai-api -> ai-api-fastapi /internal/model/*

from fastapi import APIRouter, Depends

from app.schemas.emotion import (
    EmotionClassifyRequest,
    EmotionClassifyResponse,
    RuntimeInfoResponse,
)
from app.services.emotion_service import EmotionService, get_emotion_service

router = APIRouter()


@router.get("/runtime-info", response_model=RuntimeInfoResponse)
def get_runtime_info(
    service: EmotionService = Depends(get_emotion_service),
):
    """현재 로드된 모델의 런타임 정보를 반환한다.

    ai-api가 모델 상태를 확인하거나,
    응답 메타데이터에 모델 버전을 포함할 때 사용한다.
    """
    return service.get_runtime_info()


@router.post("/emotion-classify", response_model=EmotionClassifyResponse)
def classify_emotion(
    request: EmotionClassifyRequest,
    service: EmotionService = Depends(get_emotion_service),
):
    """텍스트를 입력받아 감정분류 결과를 반환한다.

    이 엔드포인트는 감정분류 추론만 수행한다.
    상담 멘트 생성, 위험도 점수 계산, RAG 등은 ai-api의 책임이다.
    """
    return service.classify(request)
