// 파일: Diary.java
// 역할: 일기 엔티티
// 호출: DiaryService, CalendarService, ReportService -> Diary

package com.mindcompass.api.diary.domain;

import com.mindcompass.api.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "diaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "written_at", nullable = false)
    private LocalDateTime writtenAt;

    @Column(length = 50)
    private String primaryEmotion;

    @Column(name = "emotion_intensity")
    private Integer emotionIntensity;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiaryAiAnalysis> aiAnalyses = new ArrayList<>();

    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiaryEmotionTag> emotionTags = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Diary(User user, String title, String content, LocalDateTime writtenAt,
                 String primaryEmotion, Integer emotionIntensity) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.writtenAt = writtenAt;
        this.primaryEmotion = primaryEmotion;
        this.emotionIntensity = emotionIntensity;
    }

    public void update(String title, String content, LocalDateTime writtenAt) {
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
        if (writtenAt != null) {
            this.writtenAt = writtenAt;
        }
    }

    public void updateUserEmotion(String primaryEmotion, Integer emotionIntensity) {
        this.primaryEmotion = primaryEmotion;
        this.emotionIntensity = emotionIntensity;
    }

     // AI 분석 감정을 대표 감정으로 반영한다. 사용자가 직접 고른 감정이 있으면 덮어쓰지 않는다.
    public void applyAiEmotionIfAbsent(String primaryEmotion, Integer emotionIntensity) {
        if (this.primaryEmotion == null || this.primaryEmotion.isBlank()) {
            this.primaryEmotion = primaryEmotion;
            this.emotionIntensity = emotionIntensity;
        }
    }

    public void addEmotionTag(DiaryEmotionTag emotionTag) {
        this.emotionTags.add(emotionTag);
    }

    public void removeEmotionTagsBySourceType(String sourceType) {
        this.emotionTags.removeIf(tag -> tag.getSourceType().equals(sourceType));
    }

    public void addAiAnalysis(DiaryAiAnalysis aiAnalysis) {
        this.aiAnalyses.add(aiAnalysis);
    }

    public Optional<DiaryAiAnalysis> getLatestAiAnalysis() {
        return this.aiAnalyses.stream()
                .max(Comparator.comparing(DiaryAiAnalysis::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())));
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }
}
