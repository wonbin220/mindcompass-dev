// 파일: DiaryListResponse.java
// 역할: 일기 목록 응답 DTO (간략 정보)
// 화면: 일기 목록, 캘린더 뷰

package com.mindcompass.api.diary.dto.response;

import com.mindcompass.api.diary.domain.Diary;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class DiaryListResponse {

    private final Long id;
    private final String title;
    private final LocalDate diaryDate;
    private final String primaryEmotion;
    private final Double emotionScore;
    private final Boolean isAnalyzed;

    public static DiaryListResponse from(Diary diary) {
        return DiaryListResponse.builder()
                .id(diary.getId())
                .title(diary.getTitle())
                .diaryDate(diary.getDiaryDate())
                .primaryEmotion(diary.getPrimaryEmotion())
                .emotionScore(diary.getEmotionScore())
                .isAnalyzed(diary.getIsAnalyzed())
                .build();
    }
}
