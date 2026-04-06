// 파일: Diary.java
// 역할: 일기 엔티티
// 테이블: diaries
// 설명: 사용자가 작성한 감정 일기를 저장한다

package com.mindcompass.api.diary.domain;

import com.mindcompass.api.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(nullable = false)
    private LocalDate diaryDate;  // 일기 날짜 (작성일과 다를 수 있음)

    // AI 분석 결과 (nullable - AI 실패해도 일기는 저장됨)
    @Column(length = 50)
    private String primaryEmotion;

    private Double emotionScore;

    @Column(length = 500)
    private String summary;

    private Integer riskScore;

    @Column(nullable = false)
    private Boolean isAnalyzed = false;  // AI 분석 완료 여부

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Diary(User user, String title, String content, LocalDate diaryDate) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.diaryDate = diaryDate;
        this.isAnalyzed = false;
    }

    // 일기 수정
    public void update(String title, String content, LocalDate diaryDate) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (diaryDate != null) this.diaryDate = diaryDate;

        // 내용이 수정되면 분석 결과 초기화
        this.isAnalyzed = false;
        this.primaryEmotion = null;
        this.emotionScore = null;
        this.summary = null;
        this.riskScore = null;
    }

    // AI 분석 결과 저장
    public void applyAnalysis(String primaryEmotion, Double emotionScore,
                              String summary, Integer riskScore) {
        this.primaryEmotion = primaryEmotion;
        this.emotionScore = emotionScore;
        this.summary = summary;
        this.riskScore = riskScore;
        this.isAnalyzed = true;
    }

    // 소유자 확인
    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }
}
