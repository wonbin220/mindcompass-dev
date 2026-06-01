#!/bin/bash
# 파일: scripts/start.sh
# 역할: 컨테이너 시작 시 모델 준비 후 FastAPI 서버 기동
# 흐름: tired_v5(S3) 다운로드 → LimYeri(HF Hub) 캐시 → uvicorn 시작

echo "=== MindCompass ai-api-fastapi 시작 ==="

TIRED_MODEL_DIR="/app/models/tired_v5"

if [ -z "$(ls -A $TIRED_MODEL_DIR 2>/dev/null)" ]; then
    echo "[1/2] S3에서 tired_v5 모델 다운로드 중..."
    python3 - <<'EOF'
import boto3, os

s3 = boto3.client("s3")
bucket = os.environ["MODEL_BUCKET"]
prefix = "models/tired_v5/"
target = "/app/models/tired_v5/"

paginator = s3.get_paginator("list_objects_v2")
for page in paginator.paginate(Bucket=bucket, Prefix=prefix):
    for obj in page.get("Contents", []):
        key = obj["Key"]
        filename = key[len(prefix):]
        if not filename:
            continue
        local_path = target + filename
        os.makedirs(os.path.dirname(local_path), exist_ok=True)
        s3.download_file(bucket, key, local_path)
        print(f"  ✓ {filename}")
print("tired_v5 다운로드 완료")
EOF
else
    echo "[1/2] tired_v5 캐시 사용 (건너뜀)"
fi

echo "[2/2] LimYeri 모델 캐시 확인 중..."
python3 - <<'EOF'
from transformers import AutoModelForSequenceClassification, AutoTokenizer

model_id = "LimYeri/HowRU-KoELECTRA-Emotion-Classifier"
print(f"  {model_id} 로딩 중...")
AutoTokenizer.from_pretrained(model_id)
AutoModelForSequenceClassification.from_pretrained(model_id)
print("  LimYeri 완료")
EOF

echo "=== 서버 시작 (port 8090) ==="
exec uvicorn app.main:app --host 0.0.0.0 --port 8090