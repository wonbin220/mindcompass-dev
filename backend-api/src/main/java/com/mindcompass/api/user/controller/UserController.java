// 파일: UserController.java
// 역할: 사용자 정보 API 컨트롤러
// 엔드포인트: /api/v1/users/**
// 화면: 마이페이지, 프로필 수정

package com.mindcompass.api.user.controller;

import com.mindcompass.api.common.response.ApiResponse;
import com.mindcompass.api.common.security.CurrentUser;
import com.mindcompass.api.user.dto.request.UpdateProfileRequest;
import com.mindcompass.api.user.dto.response.UserResponse;
import com.mindcompass.api.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회
     * GET /api/v1/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(@CurrentUser Long userId) {
        UserResponse response = userService.getUser(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 프로필 수정
     * PATCH /api/v1/users/me
     */
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @CurrentUser Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse response = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "프로필이 수정되었습니다"));
    }

    /**
     * 회원 탈퇴
     * DELETE /api/v1/users/me
     */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@CurrentUser Long userId) {
        userService.deactivateUser(userId);
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다"));
    }
}
