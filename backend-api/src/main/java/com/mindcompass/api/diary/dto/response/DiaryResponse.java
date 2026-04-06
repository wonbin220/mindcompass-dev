// 파일: DiaryResponse.java
// 역할: 일기 응답 DTO
// 화면: 일기 상세 페이지, 일기 목록

package com.mindcompass.api.diary.dto.response;

import com.mindcompass.api.diary.domain.Diary;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class DiaryResponse {

    private final Long id;
    private final String title;
    private final String content;
    private final LocalDate diaryDate;

    // AI 분석 결과
    private final String primaryEmotion;
    private final Double emotionScore;
    private final String summary;
    private final Integer riskScore;
    private final Boolean isAnalyzed;

    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static DiaryResponse from(Diary diary) {
        return DiaryResponse.builder()
                .id(diary.getId())
                .title(diary.getTitle())
                .content(diary.getContent())
                .diaryDate(diary.getDiaryDate())
                .primaryEmotion(diary.getPrimaryEmotion())
                .emotionScore(diary.getEmotionScore())
                .summary(diary.getSummary())
                .riskScore(diary.getRiskScore())
                .isAnalyzed(diary.getIsAnalyzed())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }
}
