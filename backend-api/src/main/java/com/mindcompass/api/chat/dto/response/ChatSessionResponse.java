// 파일: ChatSessionResponse.java
// 역할: 채팅 세션 응답 DTO
// 화면: 채팅 목록, 채팅 상세

package com.mindcompass.api.chat.dto.response;

import com.mindcompass.api.chat.domain.ChatSession;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatSessionResponse {

    private final Long id;
    private final String title;
    private final String status;
    private final int messageCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static ChatSessionResponse from(ChatSession session) {
        return ChatSessionResponse.builder()
                .id(session.getId())
                .title(session.getTitle())
                .status(session.getStatus().name())
                .messageCount(session.getMessages().size())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}
