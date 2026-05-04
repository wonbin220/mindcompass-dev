// 파일: UpdateProfileRequest.java
// 역할: 프로필 수정 요청 DTO
// 호출: UserController -> UserService

package com.mindcompass.api.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "닉네임은 2~50자여야 합니다")
    private String nickname;
}
