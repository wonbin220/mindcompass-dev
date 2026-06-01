# 파일: ai-api-fastapi/Dockerfile
# 역할: FastAPI 감정분류 서버 컨테이너 이미지
# 전략: 모델 파일 없이 빌드 → 시작 시 S3/HF Hub에서 로딩

FROM python:3.10-slim

WORKDIR /app

# torch CPU 버전 먼저 설치 (CUDA 버전 방지)
COPY requirements.txt .
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu && \
  pip install --no-cache-dir -r requirements.txt

# 앱 코드 복사 (training/ 는 .dockerignore 에서 제외됨)
COPY app/ ./app/
COPY scripts/start.sh /start.sh

RUN chmod +x /start.sh && \
  mkdir -p /app/models/tired_v5

# HuggingFace 캐시 경로 고정
ENV HF_HOME=/app/.cache/huggingface
ENV PYTHONUNBUFFERED=1

EXPOSE 8090

CMD ["/start.sh"]