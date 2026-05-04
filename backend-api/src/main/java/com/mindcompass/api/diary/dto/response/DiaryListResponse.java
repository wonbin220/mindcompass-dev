// 파일: DiaryListResponse.java
// 역할: 일기 목록 응답 DTO
// 호출: DiaryService, CalendarService -> DiaryController, CalendarController

package com.mindcompass.api.diary.dto.response;

import com.mindcompass.api.diary.domain.Diary;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DiaryListResponse {

    private final Long id;
    private final String title;
    private final LocalDateTime writtenAt;
    private final String primaryEmotion;
    private final Integer emotionIntensity;

    public static DiaryListResponse from(Diary diary) {
        return DiaryListResponse.builder()
                .id(diary.getId())
                .title(diary.getTitle())
                .writtenAt(diary.getWrittenAt())
                .primaryEmotion(diary.getPrimaryEmotion())
                .emotionIntensity(diary.getEmotionIntensity())
                .build();
    }
}
