// 파일: ChatRequest.java
// 역할: 채팅 AI 응답 요청 DTO
// 설명: ai-api로 채팅 응답 생성을 요청할 때 사용하는 DTO

package com.mindcompass.api.infra.ai.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatRequest {

    private final Long sessionId;
    private final Long userId;
    private final String userMessage;
    private final List<MessageHistory> history;

    @Getter
    @Builder
    public static class MessageHistory {
        private final String role;    // "user" or "assistant"
        private final String content;
    }
}
