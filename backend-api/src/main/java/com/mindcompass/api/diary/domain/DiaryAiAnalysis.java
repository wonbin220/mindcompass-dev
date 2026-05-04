// 파일: DiaryAiAnalysis.java
// 역할: 일기 AI 분석 엔티티
// 호출: DiaryService -> DiaryAiAnalysis

package com.mindcompass.api.diary.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "diary_ai_analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class DiaryAiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @Column(name = "primary_emotion", length = 30)
    private String primaryEmotion;

    @Column(name = "emotion_intensity")
    private Integer emotionIntensity;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(precision = 10, scale = 3)
    private BigDecimal confidence;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "risk_score", precision = 10, scale = 3)
    private BigDecimal riskScore;

    @Column(name = "risk_signals", columnDefinition = "TEXT")
    private String riskSignals;

    @Column(name = "recommended_action", length = 40)
    private String recommendedAction;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public DiaryAiAnalysis(Diary diary, String primaryEmotion, Integer emotionIntensity, String summary,
                           BigDecimal confidence, String rawPayload, String riskLevel,
                           BigDecimal riskScore, String riskSignals, String recommendedAction) {
        this.diary = diary;
        this.primaryEmotion = primaryEmotion;
        this.emotionIntensity = emotionIntensity;
        this.summary = summary;
        this.confidence = confidence;
        this.rawPayload = rawPayload;
        this.riskLevel = riskLevel;
        this.riskScore = riskScore;
        this.riskSignals = riskSignals;
        this.recommendedAction = recommendedAction;
    }
}
