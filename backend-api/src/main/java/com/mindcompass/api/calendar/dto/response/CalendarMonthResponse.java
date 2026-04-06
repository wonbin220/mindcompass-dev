// 파일: CalendarMonthResponse.java
// 역할: 캘린더 월별 응답 DTO
// 화면: 캘린더 뷰 (월간)

package com.mindcompass.api.calendar.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class CalendarMonthResponse {

    private final int year;
    private final int month;
    private final List<CalendarDayResponse> days;
    private final Map<String, Long> emotionSummary;  // 감정별 일기 수
    private final int totalDiaries;                   // 총 일기 수

    public static CalendarMonthResponse of(int year, int month,
                                           List<CalendarDayResponse> days,
                                           Map<String, Long> emotionSummary) {
        int total = (int) days.stream().filter(CalendarDayResponse::getHasDiary).count();

        return CalendarMonthResponse.builder()
                .year(year)
                .month(month)
                .days(days)
                .emotionSummary(emotionSummary)
                .totalDiaries(total)
                .build();
    }
}
