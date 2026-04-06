// 파일: CalendarDayResponse.java
// 역할: 캘린더 일별 응답 DTO
// 화면: 캘린더 뷰 (달력)

package com.mindcompass.api.calendar.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class CalendarDayResponse {

    private final LocalDate date;
    private final Long diaryId;           // 일기가 있으면 ID, 없으면 null
    private final String primaryEmotion;  // 해당 날짜의 감정
    private final Double emotionScore;    // 감정 강도
    private final Boolean hasDiary;       // 일기 존재 여부

    public static CalendarDayResponse empty(LocalDate date) {
        return CalendarDayResponse.builder()
                .date(date)
                .hasDiary(false)
                .build();
    }

    public static CalendarDayResponse of(LocalDate date, Long diaryId,
                                          String emotion, Double score) {
        return CalendarDayResponse.builder()
                .date(date)
                .diaryId(diaryId)
                .primaryEmotion(emotion)
                .emotionScore(score)
                .hasDiary(true)
                .build();
    }
}
