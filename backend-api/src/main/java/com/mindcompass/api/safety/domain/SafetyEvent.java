// 파일: SafetyEvent.java
// 역할: 안전 이벤트 엔티티
// 호출: Safety 흐름 -> SafetyEvent

package com.mindcompass.api.safety.domain;

import com.mindcompass.api.chat.domain.ChatMessage;
import com.mindcompass.api.chat.domain.ChatSession;
import com.mindcompass.api.diary.domain.Diary;
import com.mindcompass.api.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "safety_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class SafetyEvent {

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

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel;

    @Column(name = "risk_score", precision = 10, scale = 3)
    private BigDecimal riskScore;

    @Column(name = "trigger_signals", columnDefinition = "TEXT")
    private String triggerSignals;

    @Column(name = "action_taken", nullable = false, length = 40)
    private String actionTaken;

    @Column(nullable = false)
    private Boolean resolved = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Builder
    public SafetyEvent(User user, Diary diary, ChatSession chatSession, ChatMessage chatMessage,
                       String sourceType, String riskLevel, BigDecimal riskScore, String triggerSignals,
                       String actionTaken, Boolean resolved, LocalDateTime resolvedAt) {
        this.user = user;
        this.diary = diary;
        this.chatSession = chatSession;
        this.chatMessage = chatMessage;
        this.sourceType = sourceType;
        this.riskLevel = riskLevel;
        this.riskScore = riskScore;
        this.triggerSignals = triggerSignals;
        this.actionTaken = actionTaken;
        this.resolved = resolved != null ? resolved : false;
        this.resolvedAt = resolvedAt;
    }
}
