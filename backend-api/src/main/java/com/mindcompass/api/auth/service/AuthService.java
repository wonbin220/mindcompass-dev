// 파일: AuthService.java
// 역할: 인증 비즈니스 로직
// 흐름: AuthController -> AuthService -> UserRepository, JwtTokenProvider

package com.mindcompass.api.auth.service;

import com.mindcompass.api.auth.dto.request.LoginRequest;
import com.mindcompass.api.auth.dto.request.SignupRequest;
import com.mindcompass.api.auth.dto.response.TokenResponse;
import com.mindcompass.api.common.exception.BusinessException;
import com.mindcompass.api.common.exception.ErrorCode;
import com.mindcompass.api.common.security.JwtTokenProvider;
import com.mindcompass.api.user.domain.User;
import com.mindcompass.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.access-token-validity-ms}")
    private long accessTokenValidityMs;

    /**
     * 회원가입
     */
    @Transactional
    public void signup(SignupRequest request) {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 비밀번호 암호화 후 저장
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .build();

        userRepository.save(user);
        log.info("회원가입 완료: email={}", request.getEmail());
    }

    /**
     * 로그인
     */
    public TokenResponse login(LoginRequest request) {
        // 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        log.info("로그인 성공: userId={}", user.getId());

        return TokenResponse.of(
                accessToken,
                refreshToken,
                accessTokenValidityMs / 1000
        );
    }

    /**
     * 토큰 갱신
     */
    public TokenResponse refresh(String refreshTokenHeader) {
        // "Bearer " 제거
        String refreshToken = refreshTokenHeader.replace("Bearer ", "");

        // 토큰 검증 및 userId 추출
        jwtTokenProvider.validateToken(refreshToken);
        Long userId = jwtTokenProvider.getUserId(refreshToken);

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 새 토큰 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        log.info("토큰 갱신 완료: userId={}", userId);

        return TokenResponse.of(
                newAccessToken,
                newRefreshToken,
                accessTokenValidityMs / 1000
        );
    }
}
