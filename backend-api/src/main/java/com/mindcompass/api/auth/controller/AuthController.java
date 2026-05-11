// 파일: AuthController.java
// 역할: 인증 관련 API 컨트롤러 — HttpOnly 쿠키 방식
// 엔드포인트: /api/v1/auth/**
// 화면: 회원가입, 로그인, 로그아웃, 토큰갱신

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
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;

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

        // SameSite 지원을 위해 ResponseCookie 사용
        ResponseCookie accessCookie = ResponseCookie.from("access_token", token.getAccessToken())
                .httpOnly(true)
                .path("/")
                .maxAge(60 * 60)
                .sameSite("Lax")       // ← CSRF 방어
                .secure(false)          // ← 로컬은 false, 운영은 true
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", token.getRefreshToken())
                .httpOnly(true)
                .path("/api/v1/auth/refresh")
                .maxAge(60 * 60 * 24 * 7)
                .sameSite("Lax")
                .secure(false)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

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

        ResponseCookie accessCookie = ResponseCookie.from("access_token", token.getAccessToken())
                .httpOnly(true)
                .path("/")
                .maxAge(60 * 60)
                .sameSite("Lax")       // ← CSRF 방어
                .secure(false)          // ← 로컬은 false, 운영은 true
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", token.getRefreshToken())
                .httpOnly(true)
                .path("/api/v1/auth/refresh")
                .maxAge(60 * 60 * 24 * 7)
                .sameSite("Lax")
                .secure(false)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok(ApiResponse.success("토큰 갱신 성공"));
    }

    /**
     * 로그아웃
     * POST /api/v1/auth/logout
     * access_token, refresh_token 쿠키를 max-age=0으로 삭제
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        ResponseCookie accessCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)          // ← 0 = 브라우저가 즉시 쿠키 삭제
                .sameSite("Lax")
                .secure(false)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .path("/api/v1/auth/refresh")
                .maxAge(0)          // ← 0 = 즉시 삭제
                .sameSite("Lax")
                .secure(false)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok(ApiResponse.success("로그아웃 완료"));
    }


}
