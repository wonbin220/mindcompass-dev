// 파일: DiaryEmotionTag.java
// 역할: 일기 감정 태그 엔티티
// 호출: DiaryService -> DiaryEmotionTag

package com.mindcompass.api.diary.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "diary_emotion_tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class DiaryEmotionTag {

    public static final String SOURCE_USER = "USER";
    public static final String SOURCE_AI_ANALYSIS = "AI_ANALYSIS";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @Column(name = "emotion_code", nullable = false, length = 30)
    private String emotionCode;

    @Column
    private Integer intensity;

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public DiaryEmotionTag(Diary diary, String emotionCode, Integer intensity, String sourceType) {
        this.diary = diary;
        this.emotionCode = emotionCode;
        this.intensity = intensity;
        this.sourceType = sourceType;
    }
}
