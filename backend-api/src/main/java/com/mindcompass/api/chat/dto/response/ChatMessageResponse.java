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
    // 응답 유형: NORMAL/SAFETY/SUPPORTIVE/FALLBACK (프론트 말풍선 색 구분용)
    // 실시간 전송 응답에만 채워지고, 과거 메시지 조회(from)에서는 null이다.
    private final String responseType;
    private final LocalDateTime createdAt;

    public static ChatMessageResponse from(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .role(message.getRole().name().toLowerCase())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }

    // 메시지 전송 응답: 분기 유형(responseType)을 함께 내려 말풍선 색을 구분하게 한다
    public static ChatMessageResponse of(ChatMessage message, String responseType) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .role(message.getRole().name().toLowerCase())
                .content(message.getContent())
                .responseType(responseType)
                .createdAt(message.getCreatedAt())
                .build();
    }
}
