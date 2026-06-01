 ---
  # Docker 로컬 실행 트러블슈팅 기록

  날짜: 2026-06-01

  ---

  ## 최종 결론

  Spring Boot 4.0에서 `spring-boot-flyway` 모듈이 별도 분리됐다.
  `flyway-core`만 있으면 `FlywayAutoConfiguration`이 로딩되지 않아서
  Flyway 자체가 실행되지 않는다.

  ---

  ## 에러 1: 테이블이 하나도 없음

  ### 증상

  ```bash
  docker exec mindcompass-dev_postgres_1 psql -U mindcompass -d mindcompass \
    -c "SELECT * FROM information_schema.tables WHERE table_schema='public';"
  # (0 rows)

  원인

  Flyway가 실행되지 않아서 V001~V009 마이그레이션이 한 번도 돌지 않은 상태.
  Spring Boot 로그에 Flyway 출력이 전혀 없었다.

  진짜 원인 (에러 4에서 해결됨)

  Spring Boot 4.0이 spring-boot-flyway 모듈을 별도 분리했기 때문.
  이 모듈이 없으면 FlywayAutoConfiguration이 클래스패스에 올라오지 않는다.

  ---
  에러 2: ContainerConfig KeyError

  증상

  KeyError: 'ContainerConfig'
  File ".../compose/service.py", line 1579, in get_container_data_volumes
      container.image_config['ContainerConfig'].get('Volumes') or {}

  원인

  docker-compose v1.29.2 (Ubuntu 기본 패키지)가 최신 Docker Engine과 호환되지 않음.
  최신 Docker는 이미지 메타데이터에서 ContainerConfig 키를 제거했는데,
  docker-compose v1은 이 키를 그대로 참조한다.

  컨테이너를 재생성(recreate)할 때 이전 컨테이너의 볼륨 정보를 읽는 과정에서 발생.

  해결

  이전 컨테이너를 완전히 삭제하고 새로 생성하면 재생성 코드를 타지 않아서 우회 가능.

  docker rm -f $(docker ps -a --filter "name=mindcompass" -q)
  docker-compose up backend-api

  근본 해결책
  Docker Compose v2 플러그인으로 업그레이드.
  Ubuntu 기본 레포에는 없고 Docker 공식 레포에서 설치해야 한다.

  ---
  에러 3: WeakKeyException (JWT_SECRET 0 bits)

  증상

  Caused by: io.jsonwebtoken.security.WeakKeyException:
  The specified key byte array is 0 bits which is not secure enough
  for any JWT HMAC-SHA algorithm.

  원인

  docker-compose.yml에서 JWT_SECRET: ${JWT_SECRET}으로 참조하는데,
  .env 파일이 없거나 환경변수가 설정되지 않으면 빈 문자열이 전달된다.
  JJWT 0.12.5는 256 bits(32자) 미만의 키를 거부한다.

  # docker-compose.yml
  backend-api:
    environment:
      JWT_SECRET: ${JWT_SECRET}  # 환경변수 미설정 시 빈 문자열

  해결

  프로젝트 루트에 .env 파일 생성 (git에 올리면 절대 안 됨):

  JWT_SECRET=32자-이상의-충분히-긴-시크릿-값

  .gitignore에 .env 추가 확인 필수.

  ---
  에러 4 (핵심): Flyway AutoConfiguration 미로딩

  증상

  - Spring Boot 로그에 Flyway 관련 출력이 전혀 없음
  - Schema validation: missing table [ai_audit_logs] 에러 발생
  - DB에 테이블이 하나도 없음

  원인

  Spring Boot 4.0에서 autoconfiguration이 기술 영역별 모듈로 쪼개졌다.

  Spring Boot 3.x에서는 spring-boot-autoconfigure 하나에 모든 자동설정이 있었지만,
  4.0부터는 아래처럼 분리됐다:

  ┌───────────────────────┬────────────────────────────────────────────────┐
  │         모듈          │                      역할                      │
  ├───────────────────────┼────────────────────────────────────────────────┤
  │ spring-boot-hibernate │ Hibernate/JPA 자동설정                         │
  ├───────────────────────┼────────────────────────────────────────────────┤
  │ spring-boot-jpa       │ JPA 추상화                                     │
  ├───────────────────────┼────────────────────────────────────────────────┤
  │ spring-boot-jdbc      │ JDBC 자동설정                                  │
  ├───────────────────────┼────────────────────────────────────────────────┤
  │ spring-boot-flyway    │ Flyway 자동설정 ← 이게 없으면 Flyway 안 돌아감 │
  └───────────────────────┴────────────────────────────────────────────────┘

  flyway-core와 flyway-database-postgresql만 추가해도
  FlywayAutoConfiguration이 클래스패스에 올라오지 않는다.

  진단 방법

  JAR 내부에서 spring-boot 모듈 목록 확인:

  docker-compose run --rm --entrypoint bash backend-api -c \
    "grep -ao 'BOOT-INF/lib/spring-boot[^/]*\.jar' /app/app.jar | sort -u"

  spring-boot-flyway-4.0.5.jar가 없으면 이 문제다.

  해결

  build.gradle에 spring-boot-flyway 모듈 추가:

  // 기존
  implementation 'org.flywaydb:flyway-core'
  implementation 'org.flywaydb:flyway-database-postgresql'

  // 수정
  implementation 'org.springframework.boot:spring-boot-flyway'  // ← 이게 핵심
  implementation 'org.flywaydb:flyway-core'
  implementation 'org.flywaydb:flyway-database-postgresql'

  이후 이미지 재빌드:

  docker rm -f $(docker ps -a --filter "name=mindcompass" -q)
  docker-compose build --no-cache backend-api
  docker-compose up backend-api

  ---

   최종 결과

   public | ai_audit_logs         | table | mindcompass
   public | chat_messages         | table | mindcompass
   public | chat_sessions         | table | mindcompass
   public | diaries               | table | mindcompass
   public | diary_ai_analyses     | table | mindcompass
   public | diary_emotion_tags    | table | mindcompass
   public | flyway_schema_history | table | mindcompass
   public | refresh_tokens        | table | mindcompass
   public | safety_events         | table | mindcompass
   public | user_settings         | table | mindcompass
   public | users                 | table | mindcompass
  (11 rows)

  회원가입, 로그인, 중복 이메일 방어, User Enumeration 방어 전부 정상 동작 확인.