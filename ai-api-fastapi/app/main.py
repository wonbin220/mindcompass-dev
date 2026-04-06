# 파일: main.py
# 역할: FastAPI 앱 생성 및 라우터 등록
# 호출: uvicorn app.main:app 으로 실행

from fastapi import FastAPI

from app.routers import health, model

app = FastAPI(
    title="Mind Compass Emotion Model API",
    description="감정분류 모델 서빙 계층. ai-api 내부 호출 전용.",
    version="0.1.0",
    docs_url="/internal/docs",
    redoc_url="/internal/redoc",
)

app.include_router(health.router)
app.include_router(model.router, prefix="/internal/model")
