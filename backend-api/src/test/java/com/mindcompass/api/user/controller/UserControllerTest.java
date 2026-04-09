package com.mindcompass.api.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcompass.api.common.security.JwtTokenProvider;
import com.mindcompass.api.user.dto.request.UpdateProfileRequest;
import com.mindcompass.api.user.dto.response.UserResponse;
import com.mindcompass.api.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("내 정보 조회 요청 성공")
    void getMyProfile_success() throws Exception {
        // given
        Long userId = 1L;
        setupSecurityContext(userId);
        UserResponse response = createUserResponse(userId);
        given(userService.getUser(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/users/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.email").value(response.getEmail()))
                .andExpect(jsonPath("$.data.name").value(response.getName()));
    }

    @Test
    @DisplayName("프로필 수정 요청 성공")
    void updateProfile_success() throws Exception {
        // given
        Long userId = 1L;
        setupSecurityContext(userId);
        UpdateProfileRequest request = createUpdateProfileRequest("newName", "newImageUrl");
        UserResponse response = createUserResponse(userId);
        ReflectionTestUtils.setField(response, "name", "newName");
        ReflectionTestUtils.setField(response, "profileImageUrl", "newImageUrl");

        given(userService.updateProfile(any(), any(UpdateProfileRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("newName"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("newImageUrl"))
                .andExpect(jsonPath("$.message").value("프로필이 수정되었습니다"));
    }

    @Test
    @DisplayName("회원 탈퇴 요청 성공")
    void deleteAccount_success() throws Exception {
        // given
        Long userId = 1L;
        setupSecurityContext(userId);
        doNothing().when(userService).deactivateUser(any());

        // when & then
        mockMvc.perform(delete("/api/v1/users/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원 탈퇴가 완료되었습니다"));
    }

    private void setupSecurityContext(Long userId) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private UserResponse createUserResponse(Long userId) {
        return UserResponse.builder()
                .id(userId)
                .email("test@test.com")
                .name("testName")
                .profileImageUrl("imageUrl")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private UpdateProfileRequest createUpdateProfileRequest(String name, String profileImageUrl) {
        UpdateProfileRequest request = new UpdateProfileRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "profileImageUrl", profileImageUrl);
        return request;
    }
}
