package com.mindcompass.api.chat.service;

import com.mindcompass.api.chat.domain.ChatMessage;
import com.mindcompass.api.chat.domain.ChatSession;
import com.mindcompass.api.chat.domain.MessageRole;
import com.mindcompass.api.chat.domain.SessionStatus;
import com.mindcompass.api.chat.dto.request.SendMessageRequest;
import com.mindcompass.api.chat.dto.response.ChatMessageResponse;
import com.mindcompass.api.chat.dto.response.ChatSessionResponse;
import com.mindcompass.api.chat.repository.ChatMessageRepository;
import com.mindcompass.api.chat.repository.ChatSessionRepository;
import com.mindcompass.api.infra.ai.AiChatClient;
import com.mindcompass.api.infra.ai.AiSafetyClient;
import com.mindcompass.api.infra.ai.dto.ChatRequest;
import com.mindcompass.api.infra.ai.dto.ChatResponse;
import com.mindcompass.api.infra.ai.dto.SafetyCheckResponse;
import com.mindcompass.api.user.domain.User;
import com.mindcompass.api.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock
    private ChatSessionRepository sessionRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AiSafetyClient aiSafetyClient;

    @Mock
    private AiChatClient aiChatClient;

    @Test
    @DisplayName("채팅 세션 생성 성공")
    void createSession_success() {
        // given
        Long userId = 1L;
        User user = createUser(userId);
        ChatSession savedSession = createSession(user);
        ReflectionTestUtils.setField(savedSession, "createdAt", LocalDateTime.of(2026, 4, 8, 17, 0));
        ReflectionTestUtils.setField(savedSession, "updatedAt", LocalDateTime.of(2026, 4, 8, 17, 0));

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(sessionRepository.save(any(ChatSession.class))).willReturn(savedSession);

        // when
        ChatSessionResponse response = chatService.createSession(userId);

        // then
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(SessionStatus.ACTIVE.name());
        assertThat(response.getMessageCount()).isZero();
        verify(sessionRepository).save(any(ChatSession.class));
    }

    @Test
    @DisplayName("메시지 전송 성공 - 일반 상황에서는 AI 응답을 생성한다")
    void sendMessage_success_generatesAiReply() {
        // given
        Long userId = 1L;
        Long sessionId = 100L;
        User user = createUser(userId);
        ChatSession session = createSession(user);
        SendMessageRequest request = createSendMessageRequest("오늘 하루가 너무 불안했어요");
        ChatMessage previousAssistantMessage = createMessage(10L, session, MessageRole.ASSISTANT, "어떤 일이 있었는지 말씀해 주세요", false, null);
        ChatResponse aiResponse = ChatResponse.builder()
                .message("많이 불안하셨겠어요. 천천히 이야기해 볼까요?")
                .isSafetyTriggered(false)
                .safetyType(null)
                .build();
        AtomicLong messageIdSequence = new AtomicLong(1000L);

        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId))
                .willReturn(List.of(previousAssistantMessage));
        given(aiSafetyClient.checkSafety(any())).willReturn(SafetyCheckResponse.builder()
                .isRisky(false)
                .riskLevel("LOW")
                .build());
        given(aiChatClient.generateReply(any(ChatRequest.class))).willReturn(aiResponse);
        given(messageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", messageIdSequence.getAndIncrement());
            ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 4, 8, 17, 10));
            return message;
        });

        // when
        ChatMessageResponse response = chatService.sendMessage(userId, sessionId, request);

        // then
        assertThat(response.getRole()).isEqualTo("assistant");
        assertThat(response.getContent()).isEqualTo("많이 불안하셨겠어요. 천천히 이야기해 볼까요?");
        assertThat(response.getIsSafetyTriggered()).isFalse();
        assertThat(session.getTitle()).isEqualTo("오늘 하루가 너무 불안했어요");

        ArgumentCaptor<ChatRequest> chatRequestCaptor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(aiChatClient).generateReply(chatRequestCaptor.capture());
        ChatRequest chatRequest = chatRequestCaptor.getValue();
        assertThat(chatRequest.getSessionId()).isEqualTo(sessionId);
        assertThat(chatRequest.getUserId()).isEqualTo(userId);
        assertThat(chatRequest.getUserMessage()).isEqualTo("오늘 하루가 너무 불안했어요");
        assertThat(chatRequest.getHistory()).hasSize(1);
        assertThat(chatRequest.getHistory().get(0).getRole()).isEqualTo("assistant");
        assertThat(chatRequest.getHistory().get(0).getContent()).isEqualTo("어떤 일이 있었는지 말씀해 주세요");
    }

    @Test
    @DisplayName("메시지 전송 성공 - 위기 감지 시 고정 안전 메시지를 반환한다")
    void sendMessage_success_returnsSafetyMessageWhenRiskDetected() {
        // given
        Long userId = 1L;
        Long sessionId = 100L;
        User user = createUser(userId);
        ChatSession session = createSession(user);
        SendMessageRequest request = createSendMessageRequest("이제 죽고 싶어요");
        AtomicLong messageIdSequence = new AtomicLong(2000L);

        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(aiSafetyClient.checkSafety(any())).willReturn(SafetyCheckResponse.builder()
                .isRisky(true)
                .riskLevel("CRITICAL")
                .safetyMessage("지금은 혼자 계시지 말고 1393에 바로 연락해 주세요.")
                .build());
        given(messageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", messageIdSequence.getAndIncrement());
            ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 4, 8, 17, 20));
            return message;
        });

        // when
        ChatMessageResponse response = chatService.sendMessage(userId, sessionId, request);

        // then
        assertThat(response.getRole()).isEqualTo("assistant");
        assertThat(response.getContent()).isEqualTo("지금은 혼자 계시지 말고 1393에 바로 연락해 주세요.");
        assertThat(response.getIsSafetyTriggered()).isTrue();
        assertThat(response.getSafetyType()).isEqualTo("CRITICAL");
        verify(aiChatClient, never()).generateReply(any(ChatRequest.class));
    }

    @Test
    @DisplayName("메시지 전송 성공 - AI 응답 생성 실패 시 fallback 메시지를 반환한다")
    void sendMessage_success_returnsFallbackWhenAiReplyFails() {
        // given
        Long userId = 1L;
        Long sessionId = 100L;
        User user = createUser(userId);
        ChatSession session = createSession(user);
        SendMessageRequest request = createSendMessageRequest("괜찮아지고 싶어요");
        AtomicLong messageIdSequence = new AtomicLong(3000L);

        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)).willReturn(List.of());
        given(aiSafetyClient.checkSafety(any())).willReturn(SafetyCheckResponse.builder()
                .isRisky(false)
                .riskLevel("LOW")
                .build());
        given(aiChatClient.generateReply(any(ChatRequest.class)))
                .willThrow(new RuntimeException("ai-api unavailable"));
        given(messageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", messageIdSequence.getAndIncrement());
            ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 4, 8, 17, 30));
            return message;
        });

        // when
        ChatMessageResponse response = chatService.sendMessage(userId, sessionId, request);

        // then
        assertThat(response.getRole()).isEqualTo("assistant");
        assertThat(response.getContent()).isEqualTo("죄송합니다, 지금은 응답을 드리기 어려워요. 잠시 후 다시 시도해 주세요.");
        assertThat(response.getIsSafetyTriggered()).isFalse();
        assertThat(response.getSafetyType()).isNull();
        verify(aiChatClient).generateReply(any(ChatRequest.class));
    }

    @Test
    @DisplayName("채팅 세션 종료 성공")
    void closeSession_success() {
        // given
        Long userId = 1L;
        Long sessionId = 100L;
        User user = createUser(userId);
        ChatSession session = createSession(user);

        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));

        // when
        chatService.closeSession(userId, sessionId);

        // then
        assertThat(session.getStatus()).isEqualTo(SessionStatus.CLOSED);
    }

    private User createUser(Long userId) {
        User user = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .name("테스트 사용자")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private ChatSession createSession(User user) {
        ChatSession session = ChatSession.builder()
                .user(user)
                .build();
        ReflectionTestUtils.setField(session, "id", 100L);
        ReflectionTestUtils.setField(session, "messages", new java.util.ArrayList<>());
        return session;
    }

    private ChatMessage createMessage(Long id, ChatSession session, MessageRole role, String content,
                                      Boolean isSafetyTriggered, String safetyType) {
        ChatMessage message = ChatMessage.builder()
                .session(session)
                .role(role)
                .content(content)
                .isSafetyTriggered(isSafetyTriggered)
                .safetyType(safetyType)
                .build();
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 4, 8, 16, 55));
        return message;
    }

    private SendMessageRequest createSendMessageRequest(String content) {
        SendMessageRequest request = new SendMessageRequest();
        ReflectionTestUtils.setField(request, "content", content);
        return request;
    }
}
