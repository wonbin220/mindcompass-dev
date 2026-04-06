// 파일: UpdateProfileRequest.java
// 역할: 프로필 수정 요청 DTO
// 화면: 프로필 수정 페이지

package com.mindcompass.api.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 2, max = 20, message = "이름은 2~20자여야 합니다")
    private String name;

    private String profileImageUrl;
}
