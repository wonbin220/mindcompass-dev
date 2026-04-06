#!/usr/bin/env bash
# 파일: scripts/run_dev.sh
# 역할: 로컬 개발용 서버 실행 스크립트
# 사용: bash scripts/run_dev.sh

set -euo pipefail

cd "$(dirname "$0")/.."

echo "=== ai-api-fastapi 개발 서버 시작 ==="
echo "포트: 8090"
echo "문서: http://localhost:8090/internal/docs"
echo ""

uvicorn app.main:app --host 0.0.0.0 --port 8090 --reload
