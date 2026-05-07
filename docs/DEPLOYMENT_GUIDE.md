# AWS 배포 가이드

## 배포 전 체크리스트

### 1. backend-api 환경변수 (EC2에 설정)
- [ ] SPRING_PROFILES_ACTIVE=prod
- [ ] JWT_SECRET=랜덤 256비트 키 (절대 기본값 사용 금지)
- [ ] DATABASE_URL=jdbc:postgresql://RDS주소:5432/mindcompass
- [ ] DATABASE_USERNAME=
- [ ] DATABASE_PASSWORD=
- [ ] AI_API_ENABLED=true
- [ ] AI_API_BASE_URL=http://ai-api서버주소:8081

### 2. ai-api 환경변수 (EC2에 설정)
- [ ] SPRING_PROFILES_ACTIVE=manual
- [ ] OPENAI_API_KEY=sk-...

### 3. web-app (빌드 전)
- [ ] .env.production 파일 생성
- [ ] NEXT_PUBLIC_API_URL=http://EC2-IP또는도메인:8080

### 4. CORS (도메인 확정 후)
- [ ] SecurityConfig.java allowedOriginPatterns 를 * → 실제 도메인으로 변경
- [ ] 파일 위치: backend-api/src/main/

  ## 서버 포트 정리
  | 서버 | 포트 |
    |---|---|
  | web-app | 3000 |
  | backend-api | 8080 |
  | ai-api | 8081 |
  | ai-api-fastapi | 8090 |

---

1. 서비스 시작 순서 (중요!)

순서가 틀리면 연결 오류납니다.

## 서비스 시작 순서

1. RDS (PostgreSQL) — 가장 먼저
2. ai-api-fastapi — 독립 실행 가능
3. ai-api — ai-api-fastapi 에 의존
4. backend-api — RDS + ai-api 에 의존
5. web-app — backend-api 에 의존

2. 각 서비스 빌드/실행 명령어

## 빌드 및 실행 명령어

### backend-api
cd backend-api
./gradlew build -x test
java -jar build/libs/backend-api-*.jar

### ai-api
cd ai-api
./gradlew build -x test
java -jar build/libs/ai-api-*.jar

### ai-api-fastapi
cd ai-api-fastapi
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8090

### web-app
cd web-app
npm install
npm run build
npm start


3. RDS 사전 작업

## RDS 사전 작업

Spring Boot 실행 전 RDS에 DB를 먼저 만들어야 한다.
Flyway가 자동으로 테이블을 생성하지만, DB 자체는 수동 생성 필요.

  ```sql
  CREATE DATABASE mindcompass;

  Flyway 마이그레이션은 backend-api 시작 시 자동 실행됨 (V001 ~ V009).

  ### 4. ai-api-fastapi torch 주의사항

  ```markdown
  ## ai-api-fastapi 주의사항

  requirements.txt에 torch>=2.0.0 포함됨.
  설치 용량이 약 2GB 이상이므로 EC2 인스턴스 디스크 용량 확인 필요.
  최소 t3.medium 이상 권장 (메모리 4GB+).
  ```
  5. EC2 보안 그룹 포트

  ## EC2 보안 그룹 인바운드 규칙

  | 포트 | 서비스 | 허용 대상 |
  |---|---|---|
  | 22 | SSH | 내 IP만 |
  | 3000 | web-app | 0.0.0.0/0 |
  | 8080 | backend-api | 0.0.0.0/0 |
  | 8081 | ai-api | EC2 내부만 (backend-api 와 같은 서버면 불필요) |
  | 8090 | ai-api-fastapi | EC2 내부만 |
  | 5432 | RDS | EC2 보안 그룹만 |

  ---



