package com.mindcompass.api.user.service;

import com.mindcompass.api.common.exception.BusinessException;
import com.mindcompass.api.common.exception.ErrorCode;
import com.mindcompass.api.user.domain.User;
import com.mindcompass.api.user.dto.request.UpdateProfileRequest;
import com.mindcompass.api.user.dto.response.UserResponse;
import com.mindcompass.api.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("사용자 조회 성공")
    void getUser_success() {
        // given
        Long userId = 1L;
        User user = createUser(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        UserResponse response = userService.getUser(userId);

        // then
        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getEmail()).isEqualTo(user.getEmail());
        assertThat(response.getName()).isEqualTo(user.getName());
    }

    @Test
    @DisplayName("사용자 조회 실패 - 존재하지 않는 사용자")
    void getUser_fail_userNotFound() {
        // given
        Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUser(userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("프로필 수정 성공")
    void updateProfile_success() {
        // given
        Long userId = 1L;
        User user = createUser(userId);
        UpdateProfileRequest request = createUpdateProfileRequest("newName", "newImageUrl");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        UserResponse response = userService.updateProfile(userId, request);

        // then
        assertThat(response.getName()).isEqualTo("newName");
        assertThat(response.getProfileImageUrl()).isEqualTo("newImageUrl");
    }

    @Test
    @DisplayName("회원 탈퇴 성공")
    void deactivateUser_success() {
        // given
        Long userId = 1L;
        User user = createUser(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userService.deactivateUser(userId);

        // then
        assertThat(user.getStatus().name()).isEqualTo("INACTIVE");
    }

    private User createUser(Long userId) {
        User user = User.builder()
                .email("test@test.com")
                .password("password123")
                .name("testName")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.now());
        return user;
    }

    private UpdateProfileRequest createUpdateProfileRequest(String name, String profileImageUrl) {
        UpdateProfileRequest request = new UpdateProfileRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "profileImageUrl", profileImageUrl);
        return request;
    }
}
