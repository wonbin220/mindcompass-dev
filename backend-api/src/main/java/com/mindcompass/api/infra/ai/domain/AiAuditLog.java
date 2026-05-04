// 파일: AiAuditLog.java
// 역할: AI 호출 감사 로그 엔티티
// 호출: AI 연동 계층 -> AiAuditLog

package com.mindcompass.api.infra.ai.domain;

import com.mindcompass.api.chat.domain.ChatMessage;
import com.mindcompass.api.chat.domain.ChatSession;
import com.mindcompass.api.diary.domain.Diary;
import com.mindcompass.api.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AiAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id")
    private Diary diary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id")
    private ChatSession chatSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_message_id")
    private ChatMessage chatMessage;

    @Column(name = "request_type", nullable = false, length = 30)
    private String requestType;

    @Column(length = 50)
    private String provider;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @Column(name = "fallback_used", nullable = false)
    private Boolean fallbackUsed = false;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public AiAuditLog(User user, Diary diary, ChatSession chatSession, ChatMessage chatMessage,
                      String requestType, String provider, String modelName,
                      String requestPayload, String responsePayload,
                      Boolean fallbackUsed, Integer latencyMs) {
        this.user = user;
        this.diary = diary;
        this.chatSession = chatSession;
        this.chatMessage = chatMessage;
        this.requestType = requestType;
        this.provider = provider;
        this.modelName = modelName;
        this.requestPayload = requestPayload;
        this.responsePayload = responsePayload;
        this.fallbackUsed = fallbackUsed != null ? fallbackUsed : false;
        this.latencyMs = latencyMs;
    }
}
