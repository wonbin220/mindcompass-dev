// 파일: UserResponse.java
// 역할: 사용자 정보 응답 DTO
// 화면: 마이페이지, 프로필

package com.mindcompass.api.user.dto.response;

import com.mindcompass.api.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {

    private final Long id;
    private final String email;
    private final String name;
    private final String profileImageUrl;
    private final LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .profileImageUrl(user.getProfileImageUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
