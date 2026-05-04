// 파일: DiaryResponse.java
// 역할: 일기 상세 응답 DTO
// 호출: DiaryService -> DiaryController

package com.mindcompass.api.diary.dto.response;

import com.mindcompass.api.diary.domain.Diary;
import com.mindcompass.api.diary.domain.DiaryEmotionTag;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Getter
@Builder
public class DiaryResponse {

    private final Long id;
    private final String title;
    private final String content;
    private final LocalDateTime writtenAt;
    private final String primaryEmotion;
    private final Integer emotionIntensity;
    private final DiaryAiAnalysisResponse aiAnalysis;
    private final List<DiaryEmotionTagResponse> emotionTags;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static DiaryResponse from(Diary diary) {
        return DiaryResponse.builder()
                .id(diary.getId())
                .title(diary.getTitle())
                .content(diary.getContent())
                .writtenAt(diary.getWrittenAt())
                .primaryEmotion(diary.getPrimaryEmotion())
                .emotionIntensity(diary.getEmotionIntensity())
                .aiAnalysis(diary.getLatestAiAnalysis().map(DiaryAiAnalysisResponse::from).orElse(null))
                .emotionTags(diary.getEmotionTags().stream()
                        .sorted(Comparator.comparing(DiaryEmotionTag::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(DiaryEmotionTagResponse::from)
                        .toList())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }
}
