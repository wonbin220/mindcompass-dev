// 파일: TokenResponse.java
// 역할: 토큰 응답 DTO
// 설명: 로그인 성공 시 Access Token과 Refresh Token을 반환한다

package com.mindcompass.api.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponse {

    private final String accessToken;
    private final String refreshToken;
    private final Long expiresIn;     // Access Token 만료 시간 (초)
    private final String tokenType;   // "Bearer"

    public static TokenResponse of(String accessToken, String refreshToken, long expiresInSeconds) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresInSeconds)
                .tokenType("Bearer")
                .build();
    }
}
