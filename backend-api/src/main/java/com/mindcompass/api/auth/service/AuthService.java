// 파일: AuthService.java
// 역할: 인증 비즈니스 로직
// 흐름: AuthController -> AuthService -> UserRepository, JwtTokenProvider, RefreshTokenRepository

package com.mindcompass.api.auth.service;

import com.mindcompass.api.auth.domain.RefreshToken;
import com.mindcompass.api.auth.dto.request.LoginRequest;
import com.mindcompass.api.auth.dto.request.SignupRequest;
import com.mindcompass.api.auth.dto.response.TokenResponse;
import com.mindcompass.api.auth.repository.RefreshTokenRepository;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.access-token-validity-ms}")
    private long accessTokenValidityMs;

    @Value("${jwt.refresh-token-validity-ms}")
    private long refreshTokenValidityMs;

    /**
     * 회원가입
     */
    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        // save() 후 user.getId()에 생성된 ID가 채워진다.
        // 이메일은 PII(개인식별정보)이므로 로그에 남기지 않는다.
        userRepository.save(user);
        log.info("회원가입 완료: userId={}", user.getId());
    }

    /**
     * 로그인
     *
     * 기존에는 JWT를 발급만 하고 끝이었다. 이렇게 하면 서버가 토큰을 강제로 무효화할 방법이 없다.
     * 예를 들어 사용자가 로그아웃해도 훔친 refresh token으로 계속 새 access token을 발급받을 수 있다.
     * 이를 막기 위해 발급한 refresh token의 해시를 DB에 저장한다.
     * 이후 refresh 요청이 오면 DB에 등록된 토큰인지 먼저 확인한다.
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        // USER_NOT_FOUND / INVALID_PASSWORD를 구분해서 던지면 응답 코드만으로
        // 이메일 존재 여부를 공격자가 알 수 있다(사용자 열거 공격).
        // 두 경우 모두 LOGIN_FAILED(401)로 통일해 정보를 노출하지 않는다.
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 원본 토큰 대신 해시를 저장한다. DB가 유출돼도 해시만으로는 토큰을 복원할 수 없다.
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(refreshToken))
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenValidityMs / 1000))
                .build());

        log.info("로그인 성공: userId={}", user.getId());

        return TokenResponse.of(accessToken, refreshToken, accessTokenValidityMs / 1000);
    }

    /**
     * 토큰 갱신 (Token Rotation 패턴)
     *
     * 단순히 JWT 서명만 검증하면 탈취된 토큰도 통과한다.
     * 이를 막기 위해 DB 조회로 "서버가 발급한 토큰인지", "이미 사용(revoke)됐는지"를 추가로 확인한다.
     *
     * Token Rotation: 갱신할 때마다 기존 토큰을 폐기하고 새 토큰을 발급한다.
     * 탈취된 토큰으로 갱신을 시도해도, 정상 사용자가 먼저 갱신했다면 그 토큰은 이미 revoke 상태가 된다.
     */
    @Transactional
    public TokenResponse refresh(String refreshToken) {
        // 1단계: JWT 서명과 만료 시각 검증
        jwtTokenProvider.validateToken(refreshToken);
        Long userId = jwtTokenProvider.getUserId(refreshToken);

        // 2단계: DB에 등록된 토큰인지 확인 (서버가 발급하지 않은 토큰 차단)
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hashToken(refreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        // 3단계: 이미 폐기된 토큰인지 확인 (재사용 공격 차단)
        if (stored.isRevoked()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 4단계: 기존 토큰 폐기 (같은 토큰으로 두 번 갱신 불가)
        stored.revoke();

        // 5단계: 새 토큰 발급 및 DB 저장
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(newRefreshToken))
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenValidityMs / 1000))
                .build());

        log.info("토큰 갱신 완료: userId={}", userId);

        return TokenResponse.of(newAccessToken, newRefreshToken, accessTokenValidityMs / 1000);
    }

    /**
     * 로그아웃
     *
     * 쿠키 삭제만으로는 부족하다. 쿠키를 미리 복사해둔 공격자는 여전히 토큰을 갖고 있다.
     * 서버 측에서 해당 사용자의 모든 refresh token을 폐기해야 실질적인 로그아웃이 된다.
     */
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now());
        log.info("로그아웃 완료: userId={}", userId);
    }

    // refresh token을 DB에 그대로 저장하면 DB 유출 시 토큰이 바로 노출된다.
    // SHA-256 해시로 변환해서 저장하면 해시만으로는 원본 토큰을 복원할 수 없다.
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다", e);
        }
    }
}
