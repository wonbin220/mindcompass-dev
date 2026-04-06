# 파일: app/routers/health.py
# 역할: 서버 상태 확인용 헬스체크 엔드포인트
# 호출: 인프라 모니터링, ai-api 연결 확인

from fastapi import APIRouter

router = APIRouter()


@router.get("/health")
def health_check():
    """서버가 살아있는지 확인하는 엔드포인트.

    ai-api가 ai-api-fastapi 연결 상태를 확인할 때 사용한다.
    로드밸런서나 컨테이너 오케스트레이터의 헬스체크 대상이기도 하다.
    """
    return {"status": "ok", "service": "ai-api-fastapi"}
