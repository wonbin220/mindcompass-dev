# CI/CD 배포 학습 문서

## 1. Goal

이 문서의 목적은 세 가지다.

1. GitHub Actions + ECR + ECS 기반 CI/CD 파이프라인을 왜 이렇게 구성했는지 이해한다.
2. 배포 과정에서 발생한 에러들의 원인과 해결 방법을 기록한다.
3. AWS 리소스 선택 이유와 다른 옵션을 쓰지 않은 이유를 이해한다.

---

## 2. 전체 배포 아키텍처

```
GitHub (main push)
    ↓
GitHub Actions (CI/CD 파이프라인)
    ↓ Docker 이미지 4개 빌드 + 태그
ECR (Elastic Container Registry) — 이미지 저장소
    ↓
ECS Fargate (컨테이너 실행)
    ├── backend-api-service   (포트 8080)
    ├── ai-api-service        (포트 8081)
    ├── ai-api-fastapi-service(포트 8090)
    └── web-app-service       (포트 3000)
```

### 흐름 요약

1. 개발자가 main 브랜치에 push
2. GitHub Actions가 해당 서비스 경로 변경 감지
3. Docker 이미지 빌드
4. ECR에 이미지 push (태그: git commit SHA)
5. ECS Task Definition을 새 이미지 URI로 업데이트
6. ECS Service가 새 Task Definition으로 롤링 배포

---

## 3. AWS 리소스 선택 이유

### 3-1. ECR (Elastic Container Registry)

**선택 이유:**
- AWS 내부 네트워크에서 ECS가 이미지를 pull하므로 별도 인증 없이 빠르게 가져올 수 있다.
- IAM 기반 접근 제어가 가능해 보안 관리가 단순하다.
- 같은 AWS 계정 내에서 데이터 전송 비용이 없다.

**Docker Hub를 쓰지 않은 이유:**
- Docker Hub는 외부 서비스라 ECS에서 pull할 때 인터넷을 거친다 → 속도 느림, 비용 발생.
- Docker Hub 무료 플랜은 pull rate limit(6시간에 100회)이 있어 CI/CD에서 자주 실패할 수 있다.
- 민감한 서비스 이미지를 퍼블릭 레지스트리에 올리는 건 보안상 좋지 않다.

**현재 생성된 ECR 레포지토리:**

| 레포지토리 이름 | URI |
|---|---|
| mindcompass-backend-api | 851691655122.dkr.ecr.us-east-1.amazonaws.com/mindcompass-backend-api |
| mindcompass-ai-api | 851691655122.dkr.ecr.us-east-1.amazonaws.com/mindcompass-ai-api |
| mindcompass-ai-api-fastapi | 851691655122.dkr.ecr.us-east-1.amazonaws.com/mindcompass-ai-api-fastapi |
| mindcompass-web-app | 851691655122.dkr.ecr.us-east-1.amazonaws.com/mindcompass-web-app |

---

### 3-2. ECS (Elastic Container Service)

**선택 이유:**
- 컨테이너를 실행하고 관리하는 AWS 관리형 서비스다.
- 서버를 직접 운영하지 않아도 컨테이너만 정의하면 자동으로 실행된다.
- GitHub Actions와 공식 액션(`amazon-ecs-deploy-task-definition`)으로 쉽게 연동된다.

**EC2 직접 실행을 쓰지 않은 이유:**
- EC2에 직접 Docker를 설치해서 실행하면 서버 OS 관리, 패치, 모니터링을 직접 해야 한다.
- ECS Fargate는 서버 인프라 관리가 필요 없다 (serverless 컨테이너).
- 서비스가 다운되면 ECS가 자동으로 재시작해준다.

**EKS(Kubernetes)를 쓰지 않은 이유:**
- EKS는 Kubernetes 클러스터 관리가 필요해서 학습 곡선이 매우 높다.
- 서비스 4개 수준의 프로젝트에 Kubernetes는 과한 선택이다.
- ECS가 훨씬 단순하고 AWS와 통합이 잘 되어 있다.

**현재 생성된 ECS 리소스:**

| 리소스 | 이름 |
|---|---|
| 클러스터 | mindcompass-cluster |
| 서비스 | backend-api-service |
| 서비스 | ai-api-service |
| 서비스 | ai-api-fastapi-service |
| 서비스 | web-app-service |
| 태스크 정의 | backend-api (revision 1) |
| 태스크 정의 | ai-api (revision 2) |
| 태스크 정의 | ai-api-fastapi (revision 2) |
| 태스크 정의 | web-app (revision 2) |

---

### 3-3. GitHub Actions

**선택 이유:**
- 코드가 이미 GitHub에 있으므로 별도 CI/CD 서비스 연동 없이 `.github/workflows/` 파일만 추가하면 된다.
- AWS 공식 GitHub Actions 액션들(`configure-aws-credentials`, `amazon-ecr-login`, `amazon-ecs-deploy-task-definition`)이 잘 관리되고 있다.
- 무료 플랜으로 월 2,000분 제공된다.

**AWS CodePipeline을 쓰지 않은 이유:**
- CodePipeline은 설정이 복잡하고 AWS 콘솔에서만 관리된다.
- GitHub Actions는 코드와 함께 버전 관리가 된다 (`.github/workflows/` 파일).
- 팀 모두가 익숙한 GitHub UI에서 배포 상태를 바로 확인할 수 있다.

**워크플로우 파일 구조:**

```
.github/workflows/
├── deploy-backend-api.yml      # backend-api/** 변경 시 트리거
├── deploy-ai-api.yml           # ai-api/** 변경 시 트리거
├── deploy-ai-api-fastapi.yml   # ai-api-fastapi/** 변경 시 트리거
└── deploy-web-app.yml          # web-app/** 변경 시 트리거
```

각 워크플로우는 경로 기반으로 트리거되어 변경된 서비스만 재배포한다.

**GitHub Secrets 등록 목록:**

| Secret 이름 | 용도 |
|---|---|
| AWS_ACCESS_KEY_ID | AWS 인증 (ECR 접근, ECS 배포) |
| AWS_SECRET_ACCESS_KEY | AWS 인증 (ECR 접근, ECS 배포) |
| NEXT_PUBLIC_API_URL | web-app Docker 빌드 시 backend-api URL 주입 |

---

## 4. 에러 & 해결 기록

### 에러 1: ECS CannotPullContainerError

**에러 메시지:**
```
CannotPullContainerError: pull image manifest has been retried 1 time(s):
failed to resolve ref docker.io/repository-uri/image-tage:latest:
pull access denied, repository does not exist or may require authorization
```

**원인:**
ECS 태스크 정의의 이미지 URI가 `docker.io/repository-uri/image-tage:latest`라는 플레이스홀더로 설정되어 있었다.
ECS가 Docker Hub에서 이미지를 찾으려 했지만 그런 이미지는 존재하지 않아 실패했다.

올바른 이미지 URI 형태:
```
851691655122.dkr.ecr.us-east-1.amazonaws.com/mindcompass-backend-api:<태그>
```

**해결 방법:**
GitHub Actions를 실행해서 ECR에 이미지를 push하고, 워크플로우가 태스크 정의를 올바른 ECR URI로 자동 업데이트하도록 했다.

**왜 이런 상황이 생겼는가:**
태스크 정의를 AWS 콘솔에서 수동으로 만들 때 이미지 URI에 플레이스홀더를 입력했고, GitHub Actions가 한 번도 실행된 적이 없어서 올바른 ECR URI로 업데이트되지 않았다.

---

### 에러 2: GitHub Actions 수동 트리거 불가

**상황:**
워크플로우가 `push` 이벤트만 있고 `workflow_dispatch`가 없어서 GitHub UI에서 수동으로 실행할 수 없었다.

**해결 방법:**
각 워크플로우 파일의 `on:` 섹션에 `workflow_dispatch:` 추가:

```yaml
on:
  workflow_dispatch:    # 수동 트리거 추가
  push:
    branches: [main]
    paths:
      - 'backend-api/**'
      - '.github/workflows/deploy-backend-api.yml'
```

**부가 효과:**
`.github/workflows/` 경로가 각 워크플로우의 트리거 경로에 포함되어 있어서, 워크플로우 파일을 수정하고 push하는 것 자체가 4개 워크플로우를 모두 트리거했다.

---

### 에러 3: Dockerfile not found

**에러 메시지:**
```
ERROR: failed to build: failed to solve: failed to read dockerfile:
open Dockerfile: no such file or directory
```

**원인:**
`backend-api/Dockerfile`, `ai-api/Dockerfile`, `web-app/Dockerfile` 3개가 git에 커밋되지 않았다.
`ai-api-fastapi/Dockerfile`만 git에 있었다.

**확인 방법:**
```bash
git ls-files backend-api/Dockerfile ai-api/Dockerfile ai-api-fastapi/Dockerfile web-app/Dockerfile
# 결과: ai-api-fastapi/Dockerfile 만 출력됨
```

GitHub Actions는 git checkout한 파일만 사용한다.
로컬에 Dockerfile이 있어도 git에 없으면 CI 환경에는 존재하지 않는다.

**해결 방법:**
```bash
git add backend-api/Dockerfile ai-api/Dockerfile web-app/Dockerfile
git commit -m "ci: Dockerfile 3개 추가"
git push origin main
```

---

### 에러 4: GradleWrapperMain ClassNotFoundException

**에러 메시지:**
```
Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain
Caused by: java.lang.ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain
```

**원인:**
`gradle/wrapper/gradle-wrapper.jar` 파일이 git에 없었다.
루트 `.gitignore`의 구조가 문제였다:

```gitignore
*.jar                                    # 모든 .jar 파일 제외
!gradle/wrapper/gradle-wrapper.jar       # 루트 경로만 예외
```

`!gradle/wrapper/gradle-wrapper.jar` 예외는 루트 경로 기준(`./gradle/wrapper/...`)이라
`backend-api/gradle/wrapper/gradle-wrapper.jar`에는 적용되지 않았다.

**해결 방법:**
`.gitignore`에서 예외 패턴을 `**/`로 변경해 하위 디렉토리 모두에 적용되도록 수정:

```gitignore
# 변경 전
!gradle/wrapper/gradle-wrapper.jar

# 변경 후
!**/gradle/wrapper/gradle-wrapper.jar
```

`**`는 "현재 위치부터 하위 모든 경로"를 의미한다.
이렇게 하면 `backend-api/gradle/wrapper/gradle-wrapper.jar`와 `ai-api/gradle/wrapper/gradle-wrapper.jar` 모두 예외가 적용된다.

이후 git에 추가:
```bash
git add backend-api/gradle/wrapper/gradle-wrapper.jar
git add ai-api/gradle/wrapper/gradle-wrapper.jar
git add .gitignore
git commit -m "fix: gradle-wrapper.jar gitignore 예외 경로 수정"
git push origin main
```

**왜 gradle-wrapper.jar를 git에 넣어야 하는가:**
`./gradlew` 명령어는 내부적으로 `gradle/wrapper/gradle-wrapper.jar`를 실행해서 Gradle을 다운로드하고 빌드를 시작한다.
이 JAR 파일이 없으면 `gradlew` 자체가 작동하지 않는다.
CI 환경에는 Gradle이 사전 설치되어 있지 않기 때문에 반드시 git에 포함되어야 한다.

---

### 에러 5: npm ci 실패 (package-lock.json 없음)

**에러 메시지:**
```
npm error The `npm ci` command can only install with an existing package-lock.json or
npm-shrinkwrap.json with lockfileVersion >= 1.
```

**원인:**
루트 `.gitignore` 37번째 줄에 `package-lock.json`이 명시적으로 제외되어 있었다.

**왜 package-lock.json이 필요한가:**
`npm install`은 `package.json` 범위 내에서 최신 버전을 설치하므로 실행할 때마다 버전이 달라질 수 있다.
`npm ci`는 `package-lock.json`에 기록된 정확한 버전을 설치해서 CI 환경에서 항상 동일한 빌드를 보장한다.

**해결 방법:**
`.gitignore`에서 `package-lock.json` 줄 삭제 후:
```bash
git add .gitignore
git add web-app/package-lock.json
git commit -m "fix: package-lock.json gitignore 제거 및 추가"
git push origin main
```

---

## 5. 각 서비스 Dockerfile 전략

### backend-api, ai-api (Spring Boot / Spring AI)

```dockerfile
# 멀티스테이지 빌드
FROM eclipse-temurin:21-jdk-jammy AS builder
# Gradle로 JAR 빌드

FROM eclipse-temurin:21-jre-jammy
# JRE만 있는 가벼운 이미지로 실행
```

**왜 멀티스테이지인가:**
- builder 스테이지: JDK + Gradle로 컴파일
- 최종 이미지: JRE만 포함 (JDK 제외) → 이미지 크기 절반 이하로 줄어듦
- JDK에는 컴파일러, javadoc 등 런타임에 불필요한 도구가 포함되어 있다

**eclipse-temurin을 선택한 이유:**
- OpenJDK의 공식 배포판으로 LTS 지원이 안정적이다
- `openjdk:21-jdk`보다 유지보수가 활성화되어 있다

---

### ai-api-fastapi (FastAPI)

```dockerfile
FROM python:3.10-slim
# CPU-only torch 먼저 설치
RUN pip install torch --index-url https://download.pytorch.org/whl/cpu
```

**CPU-only torch를 쓰는 이유:**
- ECS Fargate는 GPU 인스턴스를 지원하지 않는다
- 기본 torch는 CUDA 버전으로 2GB+이지만, CPU 버전은 약 250MB로 훨씬 가볍다
- 감정분류 추론은 CPU에서도 충분히 빠르다 (실시간 스트리밍 아님)

---

### web-app (Next.js)

```dockerfile
# 3단계 멀티스테이지
FROM node:20-alpine AS deps     # 의존성 설치
FROM node:20-alpine AS builder  # 빌드
FROM node:20-alpine AS runner   # standalone 실행
```

**standalone 빌드를 쓰는 이유:**
`next.config.ts`에 `output: 'standalone'` 설정 시 `node_modules` 전체를 포함하지 않고
실행에 필요한 파일만 bundling된 서버를 만들어준다.
이미지 크기가 대폭 줄어든다.

**alpine 이미지를 쓰는 이유:**
`node:20-alpine`은 Alpine Linux 기반으로 `node:20` 대비 크기가 약 5배 작다.
프로덕션 런타임에는 Alpine으로 충분하다.

---

## 6. 워크플로우 배포 단계별 설명

```yaml
steps:
  # 1. 소스코드 checkout
  - uses: actions/checkout@v4

  # 2. AWS 자격증명 설정 (GitHub Secrets → AWS CLI)
  - uses: aws-actions/configure-aws-credentials@v4

  # 3. ECR 로그인 (docker login 자동화)
  - uses: aws-actions/amazon-ecr-login@v2

  # 4. Docker 빌드 + ECR push
  - run: |
      docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG ./서비스경로
      docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG

  # 5. 현재 태스크 정의 다운로드
  - run: aws ecs describe-task-definition --task-definition 이름 > task-definition.json

  # 6. 새 이미지로 태스크 정의 렌더링
  - uses: aws-actions/amazon-ecs-render-task-definition@v1

  # 7. ECS 배포 (새 태스크 정의로 서비스 업데이트)
  - uses: aws-actions/amazon-ecs-deploy-task-definition@v2
    with:
      wait-for-service-stability: true  # 배포 완료까지 대기
```

**IMAGE_TAG로 `github.sha`를 쓰는 이유:**
- `latest` 태그만 쓰면 어떤 코드가 배포됐는지 추적이 불가능하다
- commit SHA로 태그하면 어떤 커밋이 배포됐는지 ECR에서 바로 확인할 수 있다
- 롤백 시 특정 SHA 태그의 이미지로 태스크 정의를 되돌리면 된다

---

## 7. RDS 및 ECS 환경변수 설정 (2026-06-02 완료)

### 7-1. RDS (PostgreSQL) 생성

**생성한 RDS 정보:**

| 항목 | 값 |
|------|-----|
| DB 식별자 | mindcompass-dev |
| 엔드포인트 | mindcompass-dev.c2h8ms6ugbuh.us-east-1.rds.amazonaws.com |
| 포트 | 5432 |
| 마스터 사용자명 | postgres |
| DB 이름 | mindcompass |
| 인스턴스 | db.t3.micro (프리 티어) |
| 퍼블릭 액세스 | 아니요 |

**프리 티어를 선택한 이유:**
포트폴리오/학습 목적 프로젝트라 db.t3.micro로 충분하다.
운영 서비스라면 Multi-AZ, 자동 백업, 더 큰 인스턴스를 고려해야 한다.

**퍼블릭 액세스를 끈 이유:**
RDS는 ECS 컨테이너에서만 접근하면 된다.
외부에 노출하면 브루트포스, SQL 인젝션 등 공격 표면이 넓어진다.
VPC 내부 통신만 허용하는 것이 보안상 올바른 구성이다.

**보안 그룹 설정:**
RDS 보안 그룹 인바운드에 포트 5432, 소스 `0.0.0.0/0` 임시 허용.
→ 추후 ECS 보안 그룹 ID로 좁혀야 한다.

### 7-2. ECS 태스크 정의 환경변수 설정 (backend-api)

태스크 정의에 실제 등록한 환경변수:

```
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://mindcompass-dev.c2h8ms6ugbuh.us-east-1.rds.amazonaws.com:5432/mindcompass
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=<설정한 비밀번호>
JWT_SECRET=<openssl rand -base64 64 로 생성한 값>
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

**JWT_SECRET 생성 방법:**
```bash
openssl rand -base64 64
```

### 7-3. 현재 배포 완료 상태 (2026-06-02)

| 서비스 | 태스크 정의 | 상태 |
|--------|------------|------|
| ai-api-fastapi-service | ai-api-fastapi:3 | ✅ 1/1 실행 중 |
| ai-api-service | ai-api:3 | ✅ 1/1 실행 중 |
| backend-api-service | backend-api:4 | ✅ 1/1 실행 중 |
| web-app-service | web-app:3 | ✅ 1/1 실행 중 |

### 7-4. 남은 배포 작업

1. **ALB (Application Load Balancer)** — 외부 트래픽을 ECS로 전달
2. **CloudFront** — web-app CDN + `/api/**` → ALB 라우팅
3. **보안 그룹 강화** — RDS 인바운드를 ECS 보안 그룹으로 좁히기
4. **도메인 연결** (선택)

---

## 8. ECS 태스크 정의 관련 실수 & 개념 정리

### 에러 6: ECS 태스크 정의 개정 전체 삭제 실수

**상황:**
태스크 정의 개정 목록에서 "새 개정 생성" 버튼을 찾다가 기존 개정 1, 2번을 전부 삭제(비활성화)했다.

**왜 문제가 됐는가:**
ECS 서비스가 삭제된 개정을 참조하고 있어서 새 태스크를 시작할 수 없었다.
서비스 업데이트 시 "INACTIVE 태스크 정의를 참조하고 있다"는 경고가 나왔다.

**해결 방법:**
삭제된 개정은 복구가 안 된다.
태스크 정의를 새로 처음부터 생성해서 서비스를 새 개정으로 업데이트했다.

**새 태스크 정의 생성 시 필요한 설정:**

| 항목 | 값 |
|------|-----|
| 태스크 정의 패밀리 이름 | `backend-api` (워크플로우 `CONTAINER_NAME`과 일치해야 함) |
| 시작 유형 | Fargate |
| OS/아키텍처 | Linux/X86_64 |
| CPU | 0.5 vCPU |
| 메모리 | 1 GB |
| 태스크 실행 역할 | ecsTaskExecutionRole |
| 컨테이너 이름 | `backend-api` (워크플로우와 반드시 일치) |
| 이미지 URI | ECR URI + `:커밋SHA` |
| 포트 | 8080 |

**컨테이너 이름이 중요한 이유:**
GitHub Actions 워크플로우에서 `CONTAINER_NAME: backend-api`로 지정해서 이 이름으로 태스크 정의에서 컨테이너를 찾는다.
이름이 다르면 워크플로우가 "컨테이너를 찾을 수 없다"는 에러로 실패한다.

---

### 에러 7: DATABASE_USERNAME 잘못 입력

**에러 메시지:**
```
FATAL: password authentication failed for user "mindcompass"
```

**실수 내용:**
태스크 정의 환경변수에 `DATABASE_USERNAME`을 `mindcompass`로 입력했다.
RDS 마스터 사용자명은 생성 시 `postgres`로 설정했는데 착각한 것이다.

**왜 헷갈렸는가:**
RDS DB 식별자(`mindcompass-dev`)와 DB 이름(`mindcompass`)과 마스터 사용자명(`postgres`)이 전부 다르다.
이 세 가지를 혼동하기 쉽다.

| 용어 | 값 | 설명 |
|------|-----|------|
| DB 식별자 | mindcompass-dev | AWS에서 RDS 인스턴스를 구분하는 이름 |
| DB 이름 | mindcompass | PostgreSQL 안에 만든 실제 데이터베이스 이름 |
| 마스터 사용자명 | postgres | PostgreSQL 접속 계정 이름 |

**해결 방법:**
태스크 정의 새 개정을 만들어 `DATABASE_USERNAME`을 `postgres`로 수정하고
서비스를 새 개정으로 업데이트했다.

---

### 개념: ECS 태스크 정의는 불변(Immutable)이다

**핵심:**
ECS 태스크 정의는 한 번 생성하면 수정할 수 없다.
환경변수 하나를 바꿔도 반드시 새 개정(revision)이 생성된다.

```
backend-api:1  → 수정 불가 (삭제만 가능)
backend-api:2  → 수정 불가
backend-api:3  → 수정 불가
backend-api:4  → 현재 사용 중
```

개정 번호는 초기화되지 않고 계속 올라간다.
이건 정상 동작이다.

**왜 불변으로 설계했는가:**
- 배포 이력 추적이 가능하다 (어떤 개정이 언제 배포됐는지)
- 문제 발생 시 이전 개정으로 즉시 롤백 가능하다
- 개정이 바뀌면 반드시 서비스 업데이트를 해야 하므로 실수로 운영 환경이 바뀌는 걸 방지한다

---

### 개념: ECS 롤링 배포 중 2/1 상태

서비스 업데이트 시 잠깐 `2/1 태스크 실행 중`이 뜨는 경우가 있다.

```
정상 흐름:
1. 새 태스크 시작 (신규 개정)
2. 새 태스크 헬스체크 통과
3. 기존 태스크 종료
→ 1/1로 안정화
```

이 과정에서 잠깐 2개가 동시에 실행되는 것처럼 보인다.
이건 서비스 중단 없이 배포하기 위한 의도된 동작이다 (무중단 배포).

**서비스 업데이트 시 주의사항:**
- "최신" 선택 시 자동으로 최신 개정이 선택되지 않는 경우가 있다
- 반드시 개정 번호를 직접 확인하고 선택해야 한다

---

## 8. 학습 확인 퀴즈

### Q1. GitHub Actions에서 `workflow_dispatch`는 왜 필요한가?

<details><summary>답 보기</summary>

기본적으로 워크플로우는 `push` 이벤트로만 트리거된다.
`workflow_dispatch`를 추가하면 GitHub 웹 UI에서 수동으로 "Run workflow" 버튼을 클릭해 실행할 수 있다.
초기 배포나 특정 시점 재배포 시 코드 변경 없이도 배포가 필요할 때 유용하다.

</details>

---

### Q2. `gradle-wrapper.jar`를 git에 포함해야 하는 이유는?

<details><summary>답 보기</summary>

`./gradlew` 명령어는 `gradle/wrapper/gradle-wrapper.jar`를 실행해서 Gradle 바이너리를 다운로드하고 빌드를 시작한다.
CI 환경(GitHub Actions 러너)에는 Gradle이 사전 설치되어 있지 않기 때문에 이 JAR 파일이 git에 없으면 `gradlew`가 실행되지 않는다.

</details>

---

### Q3. `npm install` 대신 `npm ci`를 사용하는 이유는?

<details><summary>답 보기</summary>

`npm install`은 `package.json`의 버전 범위(`^`, `~`) 내에서 최신 버전을 설치하므로 실행 시점마다 버전이 달라질 수 있다.
`npm ci`는 `package-lock.json`에 기록된 정확한 버전을 설치해 CI 환경에서 항상 동일한 빌드를 보장한다.
또한 `npm ci`는 `node_modules`를 먼저 삭제하고 클린 설치하므로 캐시로 인한 빌드 오염이 없다.

</details>

---

### Q4. ECS에서 이미지 태그를 `latest` 대신 `github.sha`로 쓰는 이유는?

<details><summary>답 보기</summary>

`latest` 태그는 항상 덮어써지므로 어떤 코드가 현재 배포되어 있는지 추적이 불가능하다.
`github.sha`(커밋 해시)로 태그하면:
- ECR에서 커밋별로 이미지가 보존된다
- 문제 발생 시 특정 커밋 SHA의 이미지로 태스크 정의를 되돌려 롤백할 수 있다
- 어떤 커밋이 어떤 이미지를 만들었는지 추적 가능하다

</details>

---

### Q5. 멀티스테이지 Dockerfile을 사용하는 이유는?

<details><summary>답 보기</summary>

빌드 도구(JDK, Gradle, npm)는 런타임에 필요 없다.
멀티스테이지를 쓰면 빌드 산출물(JAR, .next)만 최종 이미지에 포함되고, 빌드 도구는 제외된다.
결과적으로 이미지 크기가 줄어들어 ECR 저장 비용, 이미지 pull 시간, 컨테이너 시작 시간이 모두 개선된다.

</details>
