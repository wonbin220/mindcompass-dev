// 파일: AuthController.java
// 역할: 인증 관련 API 컨트롤러 — HttpOnly 쿠키 방식
// 엔드포인트: /api/v1/auth/**
// 화면: 회원가입, 로그인

package com.mindcompass.api.auth.controller;

import com.mindcompass.api.auth.dto.request.LoginRequest;
import com.mindcompass.api.auth.dto.request.SignupRequest;
import com.mindcompass.api.auth.dto.response.TokenResponse;
import com.mindcompass.api.auth.service.AuthService;
import com.mindcompass.api.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입
     * POST /api/v1/auth/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다"));
    }

    /**
     * 로그인
     * POST /api/v1/auth/login
     * 성공 시 access_token, refresh_token을 HttpOnly 쿠키로 설정
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        TokenResponse token = authService.login(request); 

        Cookie accessCookie = new Cookie("access_token", token.getAccessToken());
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(60 * 60);

        Cookie refreshCookie = new Cookie("refresh_token", token.getRefreshToken());
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/api/v1/auth/refresh");
        refreshCookie.setMaxAge(60 * 60 * 24 * 7);

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(ApiResponse.success("로그인 성공"));
    }

    /**
     * 토큰 갱신
     * POST /api/v1/auth/refresh
     * refresh_token 쿠키를 읽어 새 access_token 쿠키 발급
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.success("refresh token이 없습니다"));
        }

        TokenResponse token = authService.refresh(refreshToken);

        Cookie accessCookie = new Cookie("access_token", token.getAccessToken());
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(60 * 60);

        response.addCookie(accessCookie);

        return ResponseEntity.ok(ApiResponse.success("토큰 갱신 성공"));
    }

    /**                                                                                                           [1/1971]
     * 로그아웃
     * POST /api/v1/auth/logout
     * access_token, refresh_token 쿠키를 max-age=0으로 삭제
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        Cookie accessCookie = new Cookie("access_token", "");
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);

        Cookie refreshCookie = new Cookie("refresh_token", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/api/v1/auth/refresh");
        refreshCookie.setMaxAge(0);

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);

        return ResponseEntity.ok(ApiResponse.success("로그아웃 완료"));
    }


}
