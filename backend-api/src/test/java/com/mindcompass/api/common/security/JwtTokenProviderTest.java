// 파일: JwtTokenProviderTest.java
// 역할: JWT 토큰 생성/검증/type 추출 단위 테스트
// 검증 포인트: type claim 분리, userId 추출, 만료/위조 토큰 거부

package com.mindcompass.api.common.security;

import com.mindcompass.api.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        // HMAC-SHA256 최소 256비트(32바이트) 이상 필요
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKeyString",
                "test-secret-key-for-unit-test-min-32-bytes!!");
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenValidityMs", 3600000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenValidityMs", 86400000L);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("access token의 type claim은 'access'다")
    void getTokenType_accessToken() {
        // given
        String token = jwtTokenProvider.createAccessToken(1L, "test@test.com");

        // when
        String tokenType = jwtTokenProvider.getTokenType(token);

        // then
        assertThat(tokenType).isEqualTo("access");
    }

    @Test
    @DisplayName("refresh token의 type claim은 'refresh'다")
    void getTokenType_refreshToken() {
        // given
        String token = jwtTokenProvider.createRefreshToken(1L);

        // when
        String tokenType = jwtTokenProvider.getTokenType(token);

        // then
        assertThat(tokenType).isEqualTo("refresh");
    }

    @Test
    @DisplayName("access token에서 userId를 정확히 추출한다")
    void getUserId_fromAccessToken() {
        // given
        String token = jwtTokenProvider.createAccessToken(42L, "user@test.com");

        // when
        Long userId = jwtTokenProvider.getUserId(token);

        // then
        assertThat(userId).isEqualTo(42L);
    }

    @Test
    @DisplayName("refresh token에서 userId를 정확히 추출한다")
    void getUserId_fromRefreshToken() {
        // given
        String token = jwtTokenProvider.createRefreshToken(99L);

        // when
        Long userId = jwtTokenProvider.getUserId(token);

        // then
        assertThat(userId).isEqualTo(99L);
    }

    @Test
    @DisplayName("만료된 토큰은 BusinessException을 던진다")
    void validateToken_expiredToken() {
        // given - 만료 시간을 -1초로 설정해 과거 시점 토큰 생성
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenValidityMs", -1000L);
        String token = jwtTokenProvider.createAccessToken(1L, "test@test.com");

        // when & then
        assertThatThrownBy(() -> jwtTokenProvider.validateToken(token))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("서명이 조작된 토큰은 BusinessException을 던진다")
    void validateToken_tamperedToken() {
        // given - 헤더와 페이로드는 유효하나 서명이 위조된 토큰
        String tamperedToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.invalid_signature_here"; // gitleaks:allow

        // when & then
        assertThatThrownBy(() -> jwtTokenProvider.validateToken(tamperedToken))
                .isInstanceOf(BusinessException.class);
    }
}
