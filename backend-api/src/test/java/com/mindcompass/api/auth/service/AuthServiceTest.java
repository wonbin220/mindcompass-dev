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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() {
        // given
        SignupRequest request = createSignupRequest();
        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");

        // when
        authService.signup(request);

        // then
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 중복된 이메일")
    void signup_fail_duplicateEmail() {
        // given
        SignupRequest request = createSignupRequest();
        given(userRepository.existsByEmail(request.getEmail())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        // given
        LoginRequest request = createLoginRequest();
        User user = createUser();
        ReflectionTestUtils.setField(authService, "accessTokenValidityMs", 3600000L);
        ReflectionTestUtils.setField(authService, "refreshTokenValidityMs", 604800000L);

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).willReturn(true);
        given(jwtTokenProvider.createAccessToken(user.getId(), user.getEmail())).willReturn("accessToken");
        given(jwtTokenProvider.createRefreshToken(user.getId())).willReturn("refreshToken");
        given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        TokenResponse response = authService.login(request);

        // then
        assertThat(response.getAccessToken()).isEqualTo("accessToken");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("로그인 실패 - 이메일 없음 (사용자 열거 공격 방지: LOGIN_FAILED로 통일)")
    void login_fail_userNotFound() {
        // given
        LoginRequest request = createLoginRequest();
        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_FAILED);
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치 (사용자 열거 공격 방지: LOGIN_FAILED로 통일)")
    void login_fail_invalidPassword() {
        // given
        LoginRequest request = createLoginRequest();
        User user = createUser();
        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_FAILED);
    }

    @Test
    @DisplayName("토큰 갱신 성공 - 기존 토큰 revoke 후 새 토큰 발급 (Token Rotation)")
    void refresh_success() {
        // given
        User user = createUser();
        RefreshToken storedToken = RefreshToken.builder()
                .user(user)
                .tokenHash("someHash")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        ReflectionTestUtils.setField(authService, "accessTokenValidityMs", 3600000L);
        ReflectionTestUtils.setField(authService, "refreshTokenValidityMs", 86400000L);

        given(jwtTokenProvider.validateToken("oldRefreshToken")).willReturn(true);
        given(jwtTokenProvider.getUserId("oldRefreshToken")).willReturn(1L);
        given(refreshTokenRepository.findByTokenHash(any())).willReturn(Optional.of(storedToken));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(jwtTokenProvider.createAccessToken(user.getId(), user.getEmail())).willReturn("newAccessToken");
        given(jwtTokenProvider.createRefreshToken(user.getId())).willReturn("newRefreshToken");
        given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        TokenResponse response = authService.refresh("oldRefreshToken");

        // then
        assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
        assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken");
        // Token Rotation 핵심: 기존 토큰이 revoke됐는지 확인
        assertThat(storedToken.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("토큰 갱신 실패 - DB에 없는 토큰 (서버가 발급하지 않은 위조 토큰)")
    void refresh_fail_tokenNotFound() {
        // given
        given(jwtTokenProvider.validateToken(any())).willReturn(true);
        given(jwtTokenProvider.getUserId(any())).willReturn(1L);
        given(refreshTokenRepository.findByTokenHash(any())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.refresh("fakeToken"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 이미 revoke된 토큰 (재사용 공격 차단)")
    void refresh_fail_revokedToken() {
        // given
        User user = createUser();
        RefreshToken revokedToken = RefreshToken.builder()
                .user(user)
                .tokenHash("someHash")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        revokedToken.revoke(); // 이미 폐기 처리

        given(jwtTokenProvider.validateToken(any())).willReturn(true);
        given(jwtTokenProvider.getUserId(any())).willReturn(1L);
        given(refreshTokenRepository.findByTokenHash(any())).willReturn(Optional.of(revokedToken));

        // when & then
        assertThatThrownBy(() -> authService.refresh("revokedToken"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("로그아웃 성공 - 해당 사용자의 모든 refresh token revoke")
    void logout_success() {
        // given
        Long userId = 1L;

        // when
        authService.logout(userId);

        // then - 쿠키 삭제만이 아니라 DB에서도 토큰을 폐기했는지 확인
        verify(refreshTokenRepository).revokeAllByUserId(eq(userId), any(LocalDateTime.class));
    }

    private SignupRequest createSignupRequest() {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "email", "test@test.com");
        ReflectionTestUtils.setField(request, "password", "password123");
        ReflectionTestUtils.setField(request, "nickname", "testName");
        return request;
    }

    private LoginRequest createLoginRequest() {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", "test@test.com");
        ReflectionTestUtils.setField(request, "password", "password123");
        return request;
    }

    private User createUser() {
        User user = User.builder()
                .email("test@test.com")
                .passwordHash("encodedPassword")
                .nickname("testName")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }
}
