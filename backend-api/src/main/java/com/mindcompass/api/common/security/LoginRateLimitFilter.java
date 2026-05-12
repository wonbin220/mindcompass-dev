// 파일: LoginRateLimitFilter.java
// 역할: 로그인 엔드포인트 Rate Limiting 필터
// 호출: Servlet Container -> LoginRateLimitFilter -> Spring Security Filter Chain

package com.mindcompass.api.common.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
// Spring Security보다 먼저 실행되도록 최상위 순서로 등록한다.
// 인증 처리 전에 차단해야 DB 조회 등 불필요한 리소스 낭비를 막을 수 있다.
@Order(1)
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    // IP별 버킷을 메모리에 관리한다.
    // 주의: 서버 재시작 시 초기화되고, 다중 서버 환경에서는 서버마다 독립적으로 카운트된다.
    // 다중 서버 환경에서는 bucket4j-redis로 교체해야 한다.
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (isLoginRequest(request)) {
            String clientIp = resolveClientIp(request);
            Bucket bucket = buckets.computeIfAbsent(clientIp, k -> newBucket());

            if (!bucket.tryConsume(1)) {
                log.warn("로그인 요청 횟수 초과: ip={}", clientIp);
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"success\":false,\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\"}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    // 1분에 최대 10회만 허용한다. 초과 시 429를 반환한다.
    // refillGreedy: 1분이 지나면 토큰이 한꺼번에 채워지는 방식 (버킷 8.x 신규 API)
    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(MAX_REQUESTS_PER_MINUTE)
                .refillGreedy(MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && LOGIN_PATH.equals(request.getRequestURI());
    }

    // X-Forwarded-For를 우선 사용한다.
    // 프록시/로드밸런서 환경에서 실제 클라이언트 IP를 가져오기 위해서다.
    // 단, X-Forwarded-For는 클라이언트가 조작할 수 있으므로
    // 신뢰할 수 있는 프록시에서만 이 헤더를 전달받도록 인프라 설정이 필요하다.
    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
