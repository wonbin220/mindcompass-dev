// 파일: CalendarDayResponse.java
// 역할: 캘린더 일별 응답 DTO (하루 최대 3개 일기 지원)
// 호출: CalendarService -> CalendarController

package com.mindcompass.api.calendar.dto.response;

import com.mindcompass.api.diary.domain.Diary;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class CalendarDayResponse {

    private final LocalDate date;
    private final List<DiaryBriefInfo> diaries; // 해당 날짜의 일기 목록 (최대 3개)
    private final Boolean hasDiary;

    @Getter
    @Builder
    public static class DiaryBriefInfo {
        private final Long id;
        private final String primaryEmotion;
    }

    public static CalendarDayResponse empty(LocalDate date) {
        return CalendarDayResponse.builder()
                .date(date)
                .diaries(List.of())
                .hasDiary(false)
                .build();
    }

    public static CalendarDayResponse of(LocalDate date, List<Diary> diaries) {
        List<DiaryBriefInfo> briefs = diaries.stream()
                .map(d -> DiaryBriefInfo.builder()
                        .id(d.getId())
                        .primaryEmotion(d.getPrimaryEmotion())
                        .build())
                .collect(Collectors.toList());

        return CalendarDayResponse.builder()
                .date(date)
                .diaries(briefs)
                .hasDiary(true)
                .build();
    }
}
