// 파일: ChatMessage.java
// 역할: 채팅 메시지 엔티티
// 테이블: chat_messages
// 설명: 개별 대화 메시지를 저장한다

package com.mindcompass.api.chat.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageRole role;  // USER or ASSISTANT

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 안전 관련 메타데이터
    @Column
    private Boolean isSafetyTriggered = false;  // 안전 분기 트리거 여부

    @Column(length = 50)
    private String safetyType;  // 안전 유형 (null이면 일반 응답)

    @Column(length = 50)
    private String detectedEmotion;  // 감지된 감정

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ChatMessage(ChatSession session, MessageRole role, String content,
                       Boolean isSafetyTriggered, String safetyType, String detectedEmotion) {
        this.session = session;
        this.role = role;
        this.content = content;
        this.isSafetyTriggered = isSafetyTriggered != null ? isSafetyTriggered : false;
        this.safetyType = safetyType;
        this.detectedEmotion = detectedEmotion;
    }

    // 사용자 메시지 생성
    public static ChatMessage userMessage(ChatSession session, String content) {
        return ChatMessage.builder()
                .session(session)
                .role(MessageRole.USER)
                .content(content)
                .build();
    }

    // AI 응답 메시지 생성
    public static ChatMessage assistantMessage(ChatSession session, String content,
                                               Boolean isSafetyTriggered, String safetyType) {
        return ChatMessage.builder()
                .session(session)
                .role(MessageRole.ASSISTANT)
                .content(content)
                .isSafetyTriggered(isSafetyTriggered)
                .safetyType(safetyType)
                .build();
    }
}
