package com.mindcompass.api.chat.controller;

import com.mindcompass.api.chat.dto.response.ChatMessageResponse;
import com.mindcompass.api.chat.dto.response.ChatSessionResponse;
import com.mindcompass.api.chat.service.ChatService;
import com.mindcompass.api.common.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("POST /api/v1/chat/sessions - 채팅 세션 생성 성공 (201)")
    void createSession_success() throws Exception {
        // given
        ChatSessionResponse response = createChatSessionResponse(1L, "오늘의 상담", "ACTIVE", 0);
        given(chatService.createSession(isNull())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/chat/sessions"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("채팅이 시작되었습니다"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("오늘의 상담"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.messageCount").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/chat/sessions - 채팅 세션 목록 조회 성공")
    void getSessions_success() throws Exception {
        // given
        ChatSessionResponse item1 = createChatSessionResponse(1L, "첫 번째 상담", "ACTIVE", 2);
        ChatSessionResponse item2 = createChatSessionResponse(2L, "두 번째 상담", "CLOSED", 4);
        Page<ChatSessionResponse> page = new PageImpl<>(
                List.of(item1, item2),
                PageRequest.of(0, 20),
                2
        );

        given(chatService.getSessions(isNull(), any())).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/v1/chat/sessions")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("첫 번째 상담"))
                .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.content[1].id").value(2))
                .andExpect(jsonPath("$.data.content[1].title").value("두 번째 상담"))
                .andExpect(jsonPath("$.data.content[1].status").value("CLOSED"))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/chat/sessions/{id} - 채팅 세션 메시지 목록 조회 성공")
    void getSessionMessages_success() throws Exception {
        // given
        ChatMessageResponse userMessage = createChatMessageResponse(10L, "user", "오늘 너무 불안해요", false, null);
        ChatMessageResponse assistantMessage = createChatMessageResponse(11L, "assistant", "많이 불안하셨겠어요.", false, null);

        given(chatService.getSessionMessages(isNull(), eq(1L)))
                .willReturn(List.of(userMessage, assistantMessage));

        // when & then
        mockMvc.perform(get("/api/v1/chat/sessions/{sessionId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].role").value("user"))
                .andExpect(jsonPath("$.data[0].content").value("오늘 너무 불안해요"))
                .andExpect(jsonPath("$.data[1].id").value(11))
                .andExpect(jsonPath("$.data[1].role").value("assistant"))
                .andExpect(jsonPath("$.data[1].content").value("많이 불안하셨겠어요."));
    }

    @Test
    @DisplayName("POST /api/v1/chat/sessions/{id}/messages - 메시지 전송 성공")
    void sendMessage_success() throws Exception {
        // given
        ChatMessageResponse response = createChatMessageResponse(
                21L,
                "assistant",
                "지금 느끼는 감정을 천천히 말해주셔도 괜찮아요.",
                false,
                null
        );
        given(chatService.sendMessage(isNull(), eq(1L), any())).willReturn(response);

        String requestJson = """
                {"content":"오늘 하루가 너무 힘들었어요"}
                """;

        // when & then
        mockMvc.perform(post("/api/v1/chat/sessions/{sessionId}/messages", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("요청이 성공적으로 처리되었습니다"))
                .andExpect(jsonPath("$.data.id").value(21))
                .andExpect(jsonPath("$.data.role").value("assistant"))
                .andExpect(jsonPath("$.data.content").value("지금 느끼는 감정을 천천히 말해주셔도 괜찮아요."))
                .andExpect(jsonPath("$.data.isSafetyTriggered").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/chat/sessions/{id}/close - 채팅 세션 종료 성공")
    void closeSession_success() throws Exception {
        // given
        doNothing().when(chatService).closeSession(isNull(), eq(1L));

        // when & then
        mockMvc.perform(post("/api/v1/chat/sessions/{sessionId}/close", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("채팅이 종료되었습니다"));
    }

    private ChatSessionResponse createChatSessionResponse(Long id, String title, String status, int messageCount) {
        LocalDateTime now = LocalDateTime.of(2026, 4, 8, 16, 0);
        return ChatSessionResponse.builder()
                .id(id)
                .title(title)
                .status(status)
                .messageCount(messageCount)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private ChatMessageResponse createChatMessageResponse(
            Long id,
            String role,
            String content,
            Boolean isSafetyTriggered,
            String safetyType
    ) {
        return ChatMessageResponse.builder()
                .id(id)
                .role(role)
                .content(content)
                .isSafetyTriggered(isSafetyTriggered)
                .safetyType(safetyType)
                .createdAt(LocalDateTime.of(2026, 4, 8, 16, 0))
                .build();
    }
}
