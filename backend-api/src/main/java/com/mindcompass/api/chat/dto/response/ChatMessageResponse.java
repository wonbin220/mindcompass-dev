// 파일: ChatMessageResponse.java
// 역할: 채팅 메시지 응답 DTO
// 호출: ChatService -> ChatController

package com.mindcompass.api.chat.dto.response;

import com.mindcompass.api.chat.domain.ChatMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponse {

    private final Long id;
    private final String role;
    private final String content;
    private final LocalDateTime createdAt;

    public static ChatMessageResponse from(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .role(message.getRole().name().toLowerCase())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
