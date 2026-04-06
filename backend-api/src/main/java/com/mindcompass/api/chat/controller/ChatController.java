// 파일: ChatController.java
// 역할: 채팅 API 컨트롤러
// 엔드포인트: /api/v1/chat/**
// 화면: 채팅 목록, 채팅 상세

package com.mindcompass.api.chat.controller;

import com.mindcompass.api.chat.dto.request.SendMessageRequest;
import com.mindcompass.api.chat.dto.response.ChatMessageResponse;
import com.mindcompass.api.chat.dto.response.ChatSessionResponse;
import com.mindcompass.api.chat.service.ChatService;
import com.mindcompass.api.common.response.ApiResponse;
import com.mindcompass.api.common.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 새 채팅 세션 생성
     * POST /api/v1/chat/sessions
     */
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> createSession(@CurrentUser Long userId) {
        ChatSessionResponse response = chatService.createSession(userId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "채팅이 시작되었습니다"));
    }

    /**
     * 채팅 세션 목록 조회
     * GET /api/v1/chat/sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<Page<ChatSessionResponse>>> getSessions(
            @CurrentUser Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ChatSessionResponse> response = chatService.getSessions(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 채팅 세션 상세 조회 (메시지 포함)
     * GET /api/v1/chat/sessions/{sessionId}
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getSessionMessages(
            @CurrentUser Long userId,
            @PathVariable Long sessionId) {
        List<ChatMessageResponse> response = chatService.getSessionMessages(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 메시지 전송 (AI 응답 포함)
     * POST /api/v1/chat/sessions/{sessionId}/messages
     */
    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @CurrentUser Long userId,
            @PathVariable Long sessionId,
            @Valid @RequestBody SendMessageRequest request) {
        ChatMessageResponse response = chatService.sendMessage(userId, sessionId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 채팅 세션 종료
     * POST /api/v1/chat/sessions/{sessionId}/close
     */
    @PostMapping("/sessions/{sessionId}/close")
    public ResponseEntity<ApiResponse<Void>> closeSession(
            @CurrentUser Long userId,
            @PathVariable Long sessionId) {
        chatService.closeSession(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.success("채팅이 종료되었습니다"));
    }
}
