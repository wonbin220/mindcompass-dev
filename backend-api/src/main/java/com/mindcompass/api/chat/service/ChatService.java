// 파일: ChatService.java
// 역할: 채팅 비즈니스 로직
// 흐름: ChatController -> ChatService -> Repository, AiSafetyClient, AiChatClient
// 핵심 원칙: Safety-first, 메시지 저장은 AI 실패와 무관하게 진행

package com.mindcompass.api.chat.service;

import com.mindcompass.api.chat.domain.*;
import com.mindcompass.api.chat.dto.request.SendMessageRequest;
import com.mindcompass.api.chat.dto.response.ChatMessageResponse;
import com.mindcompass.api.chat.dto.response.ChatSessionResponse;
import com.mindcompass.api.chat.repository.ChatMessageRepository;
import com.mindcompass.api.chat.repository.ChatSessionRepository;
import com.mindcompass.api.common.exception.BusinessException;
import com.mindcompass.api.common.exception.ErrorCode;
import com.mindcompass.api.infra.ai.AiChatClient;
import com.mindcompass.api.infra.ai.AiSafetyClient;
import com.mindcompass.api.infra.ai.dto.*;
import com.mindcompass.api.user.domain.User;
import com.mindcompass.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AiSafetyClient aiSafetyClient;
    private final AiChatClient aiChatClient;

    // 기본 안전 메시지 (AI 실패 시 fallback)
    private static final String DEFAULT_SAFETY_MESSAGE =
            "지금 많이 힘드시군요. 전문 상담이 도움이 될 수 있어요.\n" +
            "자살예방상담전화 1393\n" +
            "정신건강위기상담전화 1577-0199";

    private static final String DEFAULT_FALLBACK_MESSAGE =
            "죄송합니다, 지금은 응답을 드리기 어려워요. 잠시 후 다시 시도해 주세요.";

    /**
     * 새 채팅 세션 생성
     */
    @Transactional
    public ChatSessionResponse createSession(Long userId) {
        User user = findUserById(userId);

        ChatSession session = ChatSession.builder()
                .user(user)
                .build();

        session = sessionRepository.save(session);
        log.info("채팅 세션 생성: sessionId={}, userId={}", session.getId(), userId);

        return ChatSessionResponse.from(session);
    }

    /**
     * 세션 목록 조회
     */
    public Page<ChatSessionResponse> getSessions(Long userId, Pageable pageable) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable)
                .map(ChatSessionResponse::from);
    }

    /**
     * 세션 메시지 목록 조회
     */
    public List<ChatMessageResponse> getSessionMessages(Long userId, Long sessionId) {
        ChatSession session = findSessionById(sessionId);
        validateOwnership(session, userId);

        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    /**
     * 메시지 전송 및 AI 응답 생성
     *
     * 실행 흐름:
     * 1. 사용자 메시지 저장 (먼저!)
     * 2. Safety 확인
     * 3. 위기 상황이면 안전 메시지 반환
     * 4. 일반 상황이면 AI 응답 생성
     * 5. AI 응답 저장
     */
    @Transactional
    public ChatMessageResponse sendMessage(Long userId, Long sessionId, SendMessageRequest request) {
        ChatSession session = findSessionById(sessionId);
        validateOwnership(session, userId);

        // 1. 사용자 메시지 먼저 저장
        ChatMessage userMessage = ChatMessage.userMessage(session, request.getContent());
        messageRepository.save(userMessage);
        log.info("사용자 메시지 저장: sessionId={}", sessionId);

        // 세션 제목 업데이트 (첫 메시지인 경우)
        session.updateTitleIfEmpty(request.getContent());

        // 2. Safety 확인 (AI 실패해도 키워드 기반 fallback)
        SafetyCheckResponse safetyResult = checkSafety(userId, request.getContent());

        // 3. AI 응답 생성 또는 안전 메시지
        ChatMessage assistantMessage;
        if (safetyResult.getIsRisky() != null && safetyResult.getIsRisky()) {
            // 위기 상황: 고정 안전 메시지 반환
            String safetyMessage = safetyResult.getSafetyMessage() != null
                    ? safetyResult.getSafetyMessage()
                    : DEFAULT_SAFETY_MESSAGE;

            assistantMessage = ChatMessage.assistantMessage(
                    session,
                    safetyMessage,
                    true,
                    safetyResult.getRiskLevel()
            );
            log.warn("위기 상황 감지: sessionId={}, riskLevel={}",
                    sessionId, safetyResult.getRiskLevel());
        } else {
            // 일반 상황: AI 응답 생성
            assistantMessage = generateAiResponse(session, request.getContent());
        }

        // 4. AI 응답 저장
        messageRepository.save(assistantMessage);
        log.info("AI 응답 저장: sessionId={}, isSafety={}",
                sessionId, assistantMessage.getIsSafetyTriggered());

        return ChatMessageResponse.from(assistantMessage);
    }

    /**
     * 세션 종료
     */
    @Transactional
    public void closeSession(Long userId, Long sessionId) {
        ChatSession session = findSessionById(sessionId);
        validateOwnership(session, userId);

        session.close();
        log.info("채팅 세션 종료: sessionId={}", sessionId);
    }

    /**
     * Safety 확인 (AI 실패 시 fallback 포함)
     */
    private SafetyCheckResponse checkSafety(Long userId, String content) {
        try {
            SafetyCheckRequest request = SafetyCheckRequest.builder()
                    .userId(userId)
                    .content(content)
                    .context("chat")
                    .build();

            return aiSafetyClient.checkSafety(request);
        } catch (Exception e) {
            log.warn("Safety 확인 실패, 기본값 반환: {}", e.getMessage());
            return new SafetyCheckResponse(); // 기본값: isRisky = null
        }
    }

    /**
     * AI 응답 생성 (실패 시 fallback 메시지)
     */
    private ChatMessage generateAiResponse(ChatSession session, String userContent) {
        try {
            // 최근 대화 히스토리 조회 (최대 10개)
            List<ChatRequest.MessageHistory> history = messageRepository
                    .findBySessionIdOrderByCreatedAtAsc(session.getId())
                    .stream()
                    .limit(10)
                    .map(m -> ChatRequest.MessageHistory.builder()
                            .role(m.getRole().name().toLowerCase())
                            .content(m.getContent())
                            .build())
                    .toList();

            ChatRequest request = ChatRequest.builder()
                    .sessionId(session.getId())
                    .userId(session.getUser().getId())
                    .userMessage(userContent)
                    .history(history)
                    .build();

            ChatResponse response = aiChatClient.generateReply(request);

            return ChatMessage.assistantMessage(
                    session,
                    response.getMessage() != null ? response.getMessage() : DEFAULT_FALLBACK_MESSAGE,
                    response.getIsSafetyTriggered(),
                    response.getSafetyType()
            );

        } catch (Exception e) {
            log.warn("AI 응답 생성 실패, fallback 사용: {}", e.getMessage());
            return ChatMessage.assistantMessage(session, DEFAULT_FALLBACK_MESSAGE, false, null);
        }
    }

    // 공통 조회 메서드
    private ChatSession findSessionById(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_SESSION_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateOwnership(ChatSession session, Long userId) {
        if (!session.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.CHAT_SESSION_ACCESS_DENIED);
        }
    }
}
