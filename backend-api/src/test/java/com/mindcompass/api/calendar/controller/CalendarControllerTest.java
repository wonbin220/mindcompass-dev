package com.mindcompass.api.calendar.controller;

import com.mindcompass.api.calendar.dto.response.CalendarDayResponse;
import com.mindcompass.api.calendar.dto.response.CalendarMonthResponse;
import com.mindcompass.api.calendar.service.CalendarService;
import com.mindcompass.api.common.security.JwtTokenProvider;
import com.mindcompass.api.diary.dto.response.DiaryListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CalendarController.class)
@AutoConfigureMockMvc(addFilters = false)
class CalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CalendarService calendarService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("GET /api/v1/calendar?year=2026&month=4 - 월별 캘린더 조회 성공")
    void getMonthCalendar_success() throws Exception {
        // given
        int year = 2026;
        int month = 4;
        CalendarMonthResponse response = CalendarMonthResponse.builder()
                .year(year)
                .month(month)
                .days(List.of(
                        CalendarDayResponse.builder()
                                .date(LocalDate.of(year, month, 1))
                                .diaryId(1L)
                                .primaryEmotion("기쁨")
                                .emotionScore(0.9)
                                .hasDiary(true)
                                .build(),
                        CalendarDayResponse.empty(LocalDate.of(year, month, 2))
                ))
                .emotionSummary(Map.of("기쁨", 1L))
                .totalDiaries(1)
                .build();

        given(calendarService.getMonthCalendar(isNull(), eq(year), eq(month)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/calendar")
                        .param("year", String.valueOf(year))
                        .param("month", String.valueOf(month)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.year").value(year))
                .andExpect(jsonPath("$.data.month").value(month))
                .andExpect(jsonPath("$.data.days[0].diaryId").value(1))
                .andExpect(jsonPath("$.data.days[0].primaryEmotion").value("기쁨"))
                .andExpect(jsonPath("$.data.days[0].hasDiary").value(true))
                .andExpect(jsonPath("$.data.days[1].hasDiary").value(false))
                .andExpect(jsonPath("$.data.totalDiaries").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/calendar/{date} - 특정 날짜 일기 조회 성공")
    void getDiaryByDate_success() throws Exception {
        // given
        LocalDate date = LocalDate.of(2026, 4, 8);
        DiaryListResponse response = DiaryListResponse.builder()
                .id(1L)
                .title("오늘의 일기")
                .diaryDate(date)
                .primaryEmotion("기쁨")
                .emotionScore(0.85)
                .isAnalyzed(true)
                .build();

        given(calendarService.getDiaryByDate(isNull(), eq(date)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/calendar/{date}", date))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("오늘의 일기"))
                .andExpect(jsonPath("$.data.diaryDate").value("2026-04-08"))
                .andExpect(jsonPath("$.data.primaryEmotion").value("기쁨"));
    }

    @Test
    @DisplayName("GET /api/v1/calendar/emotion/{emotion} - 감정별 조회 성공")
    void getDiariesByEmotion_success() throws Exception {
        // given
        String emotion = "기쁨";
        int limit = 20;
        DiaryListResponse item = DiaryListResponse.builder()
                .id(1L)
                .title("기쁜 일기")
                .diaryDate(LocalDate.of(2026, 4, 8))
                .primaryEmotion(emotion)
                .emotionScore(0.95)
                .isAnalyzed(true)
                .build();

        given(calendarService.getDiariesByEmotion(isNull(), eq(emotion), eq(limit)))
                .willReturn(List.of(item));

        // when & then
        mockMvc.perform(get("/api/v1/calendar/emotion/{emotion}", emotion)
                        .param("limit", String.valueOf(limit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("기쁜 일기"))
                .andExpect(jsonPath("$.data[0].primaryEmotion").value(emotion));
    }
}
