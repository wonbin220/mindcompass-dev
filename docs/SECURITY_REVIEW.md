# 보안 리뷰 기록                                                                                             [59/1881]

Opus 4.7 + Codex 동시 검토 결과 (2026-05-11)

  ---

## 완료된 보안 작업

| 항목 | 파일 | 날짜 |
  |------|------|------|
| HttpOnly + SameSite=Lax 쿠키 | AuthController.java | 2026-05-11 |
| 비밀번호 복잡도 @Pattern | SignupRequest.java | 2026-05-11 |
| SafetyCheckRequest contextType 필드명 수정 | SafetyCheckRequest.java, ChatService.java | 2026-05-11 |
| Security 로그 DEBUG → WARN | application.yml | 2026-05-11 |
| CORS allowedOriginPatterns(*) 제거 | SecurityConfig.java | 2026-05-11 |
| Swagger 인증 필요로 변경 | SecurityConfig.java | 2026-05-11 |

  ---

## 미완료 — HIGH (다음 세션)

### 1. Refresh Token 서버 revocation
- **문제:** RefreshToken 엔티티가 DB에 있는데 저장/검증을 안 함. 로그아웃해도 탈취된 토큰이 7일간 유효
- **수정 파일:** AuthService.java, AuthController.java
- **수정 방향:**
    - login() → refreshTokenRepository.save(해시값)
    - refresh() → 기존 토큰 revoke 후 신규 발급
    - logout() → refreshTokenRepository.revokeAllByUserId()

### 2. 사용자 열거 공격 차단
- **문제:** 이메일 없으면 404, 비밀번호 틀리면 400으로 계정 존재 여부 파악 가능
- **수정 파일:** AuthService.java, ErrorCode.java
- **수정 방향:** 둘 다 401 + "이메일 또는 비밀번호가 올바르지 않습니다" 단일 응답

### 3. LIKE 와일드카드 이스케이프
- **문제:** `%` 입력 시 풀스캔 DoS 가능
- **수정 파일:** DiaryService.java, DiaryRepository.java
- **수정 방향:** %, _, \ 이스케이프 + 최소 2자/최대 50자 제한 + ESCAPE '\\\\'

### 4. Cookie Secure flag 환경별 분리
- **문제:** .secure(false) 6군데 하드코딩 → 운영에서도 HTTP로 쿠키 전송
- **수정 파일:** AuthController.java, application.yml, application-local.yml
- **수정 방향:** `@Value("${app.cookie.secure:true}")` 로 읽어서 주입

### 5. JWT type claim 검증
- **문제:** refresh token을 access token 자리에 넣어도 인증 통과
- **수정 파일:** JwtTokenProvider.java, JwtAuthenticationFilter.java
- **수정 방향:** getUserIdFromAccessToken()에서 type == "access" 검증

    ---

  ## 미완료 — MEDIUM

  ### 6. JWT 필터 401 응답
    - **문제:** 만료/위조 토큰 → 403 반환 (401이어야 함)
    - **수정 방향:** AuthenticationEntryPoint 구현, SecurityConfig에 등록

  ### 7. Security 응답 헤더
    - **문제:** CSP, HSTS, X-Frame-Options 미설정
    - **수정 방향:** SecurityConfig에 http.headers(...) 추가

  ### 8. CORS placeholder 제거
    - **문제:** yourdomain.com 하드코딩
    - **수정 방향:** app.cors.allowed-origins 환경변수로 분리

  ---

  ## 미완료 — LOW

    - AuthService.java:53 로그에 이메일(PII) → userId로 교체
    - AuthService.refresh()의 불필요한 `Bearer ` strip 코드 제거
    - application.yml 공통 프로필에서 format_sql: true 제거
    - refresh token 7일 → 24시간 검토

  ---

  ## 배포 전 체크리스트

    - [ ] CORS 실제 도메인으로 교체 (SecurityConfig.java)
    - [ ] JWT_SECRET 환경변수 설정 (기본값 제거)
    - [ ] Cookie Secure → true
    - [ ] Rate Limiting 구현 (bucket4j-core:8.10.1)
    - [ ] Refresh Token revocation 완료

  ---

  ## Codex가 확인한 "문제 없음" 항목

    - JWT secret 짧은 값 → JJWT가 초기화 시 자체 예외 발생
    - 예외 핸들러 스택 트레이스 노출 없음
    - 토큰 만료 검증 구현됨

  ---