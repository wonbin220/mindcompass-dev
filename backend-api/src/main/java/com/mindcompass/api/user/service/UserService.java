// 파일: UserService.java
// 역할: 사용자 비즈니스 로직
// 흐름: UserController -> UserService -> UserRepository

package com.mindcompass.api.user.service;

import com.mindcompass.api.common.exception.BusinessException;
import com.mindcompass.api.common.exception.ErrorCode;
import com.mindcompass.api.user.domain.User;
import com.mindcompass.api.user.dto.request.UpdateProfileRequest;
import com.mindcompass.api.user.dto.response.UserResponse;
import com.mindcompass.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * 사용자 조회
     */
    public UserResponse getUser(Long userId) {
        User user = findUserById(userId);
        return UserResponse.from(user);
    }

    /**
     * 프로필 수정
     */
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);
        user.updateProfile(request.getName(), request.getProfileImageUrl());

        log.info("프로필 수정 완료: userId={}", userId);
        return UserResponse.from(user);
    }

    /**
     * 회원 탈퇴 (소프트 삭제)
     */
    @Transactional
    public void deactivateUser(Long userId) {
        User user = findUserById(userId);
        user.deactivate();

        log.info("회원 탈퇴 완료: userId={}", userId);
    }

    // 공통 사용자 조회 메서드
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
