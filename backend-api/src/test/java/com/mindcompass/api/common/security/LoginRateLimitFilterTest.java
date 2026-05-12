// 파일: LoginRateLimitFilterTest.java
// 역할: 로그인 Rate Limiting 필터 단위 테스트
// 검증 포인트: 한도 초과 시 429, 다른 경로 미적용, IP별 독립 버킷

package com.mindcompass.api.common.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LoginRateLimitFilterTest {

    private LoginRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        // 각 테스트마다 새 인스턴스 생성 — ConcurrentHashMap 버킷 상태가 테스트 간 공유되지 않아야 한다
        filter = new LoginRateLimitFilter();
    }

    @Test
    @DisplayName("로그인 요청이 한도(10회) 이하면 필터 체인을 통과한다")
    void loginRequest_underLimit_passThrough() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    @DisplayName("같은 IP에서 1분에 10회 초과하면 429를 반환한다")
    void loginRequest_overLimit_returns429() throws Exception {
        // given - 같은 IP로 10회 소진
        FilterChain filterChain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            req.setRemoteAddr("10.0.0.1");
            filter.doFilter(req, new MockHttpServletResponse(), filterChain);
        }

        // when - 11번째 요청 (버킷 고갈)
        MockHttpServletRequest overLimitRequest = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        overLimitRequest.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse overLimitResponse = new MockHttpServletResponse();
        filter.doFilter(overLimitRequest, overLimitResponse, filterChain);

        // then
        assertThat(overLimitResponse.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("로그인 외 경로는 rate limit 대상이 아니다")
    void nonLoginPath_notRateLimited() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/diaries");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // when
        filter.doFilter(request, response, filterChain);

        // then - rate limit 없이 필터 체인 통과
        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    @DisplayName("GET /api/v1/auth/login은 rate limit 대상이 아니다 (POST만 적용)")
    void loginPath_getMethod_notRateLimited() throws Exception {
        // given - POST가 아닌 GET 요청 (rate limit은 POST 로그인에만 적용)
        FilterChain filterChain = mock(FilterChain.class);

        for (int i = 0; i < 11; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/auth/login");
            req.setRemoteAddr("10.0.0.1");
            filter.doFilter(req, new MockHttpServletResponse(), filterChain);
        }

        // when - 11번째도 GET이면 통과해야 함
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/auth/login");
        req.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(req, response, filterChain);

        // then
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    @DisplayName("IP가 다르면 서로 다른 버킷을 사용한다")
    void differentIp_separateBuckets() throws Exception {
        // given - IP A가 10회 소진
        FilterChain filterChain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            req.setRemoteAddr("10.0.0.1");
            filter.doFilter(req, new MockHttpServletResponse(), filterChain);
        }

        // when - IP B는 독립된 버킷이므로 아직 한도가 남아 있다
        MockHttpServletRequest requestB = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        requestB.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse responseB = new MockHttpServletResponse();
        filter.doFilter(requestB, responseB, filterChain);

        // then - IP B는 429가 아님
        assertThat(responseB.getStatus()).isNotEqualTo(429);
    }
}
