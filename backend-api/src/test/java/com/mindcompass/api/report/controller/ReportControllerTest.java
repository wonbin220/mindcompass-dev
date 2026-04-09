// 파일: ReportControllerTest.java
// 역할: 리포트 컨트롤러 MockMvc 테스트
// 호출: ReportController -> ReportService

package com.mindcompass.api.report.controller;

import com.mindcompass.api.common.security.JwtTokenProvider;
import com.mindcompass.api.report.dto.response.MonthlyReportResponse;
import com.mindcompass.api.report.dto.response.WeeklyReportResponse;
import com.mindcompass.api.report.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("GET /api/v1/reports/weekly - 주간 리포트 조회 성공")
    void getWeeklyReport_success() throws Exception {
        // given
        LocalDate targetDate = LocalDate.of(2026, 4, 8);
        WeeklyReportResponse response = WeeklyReportResponse.builder()
                .startDate(LocalDate.of(2026, 4, 6))
                .endDate(LocalDate.of(2026, 4, 12))
                .totalDiaries(3)
                .totalChats(2)
                .averageEmotionScore(0.72)
                .emotionDistribution(Map.of("불안", 1L, "기쁨", 2L))
                .dominantEmotion("기쁨")
                .dailyTrends(List.of(
                        WeeklyReportResponse.DailyEmotionTrend.builder()
                                .date(LocalDate.of(2026, 4, 6))
                                .emotion("기쁨")
                                .score(0.82)
                                .hasDiary(true)
                                .build(),
                        WeeklyReportResponse.DailyEmotionTrend.builder()
                                .date(LocalDate.of(2026, 4, 7))
                                .emotion(null)
                                .score(null)
                                .hasDiary(false)
                                .build(),
                        WeeklyReportResponse.DailyEmotionTrend.builder()
                                .date(LocalDate.of(2026, 4, 8))
                                .emotion("불안")
                                .score(0.54)
                                .hasDiary(true)
                                .build()
                ))
                .aiSummary(null)
                .build();

        given(reportService.getWeeklyReport(isNull(), eq(targetDate)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/reports/weekly")
                        .param("date", "2026-04-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("요청이 성공적으로 처리되었습니다"))
                .andExpect(jsonPath("$.data.startDate").value("2026-04-06"))
                .andExpect(jsonPath("$.data.endDate").value("2026-04-12"))
                .andExpect(jsonPath("$.data.totalDiaries").value(3))
                .andExpect(jsonPath("$.data.totalChats").value(2))
                .andExpect(jsonPath("$.data.averageEmotionScore").value(0.72))
                .andExpect(jsonPath("$.data.emotionDistribution.기쁨").value(2))
                .andExpect(jsonPath("$.data.emotionDistribution.불안").value(1))
                .andExpect(jsonPath("$.data.dominantEmotion").value("기쁨"))
                .andExpect(jsonPath("$.data.dailyTrends[0].date").value("2026-04-06"))
                .andExpect(jsonPath("$.data.dailyTrends[0].emotion").value("기쁨"))
                .andExpect(jsonPath("$.data.dailyTrends[0].hasDiary").value(true))
                .andExpect(jsonPath("$.data.dailyTrends[1].hasDiary").value(false))
                .andExpect(jsonPath("$.data.dailyTrends[2].emotion").value("불안"));
    }

    @Test
    @DisplayName("GET /api/v1/reports/monthly - 월간 리포트 조회 성공")
    void getMonthlyReport_success() throws Exception {
        // given
        int year = 2026;
        int month = 4;
        MonthlyReportResponse response = MonthlyReportResponse.builder()
                .year(year)
                .month(month)
                .totalDiaries(12)
                .totalChats(5)
                .averageEmotionScore(0.68)
                .emotionDistribution(Map.of("불안", 4L, "평온", 8L))
                .dominantEmotion("평온")
                .weeklySummaries(List.of(
                        MonthlyReportResponse.WeeklySummary.builder()
                                .weekNumber(1)
                                .diaryCount(4)
                                .dominantEmotion("평온")
                                .averageScore(0.74)
                                .build(),
                        MonthlyReportResponse.WeeklySummary.builder()
                                .weekNumber(2)
                                .diaryCount(3)
                                .dominantEmotion("불안")
                                .averageScore(0.58)
                                .build()
                ))
                .comparisonWithLastMonth(MonthlyReportResponse.EmotionComparison.builder()
                        .scoreDiff(0.12)
                        .diaryCountDiff(2)
                        .trend("improving")
                        .build())
                .aiInsight(null)
                .build();

        given(reportService.getMonthlyReport(isNull(), eq(year), eq(month)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/reports/monthly")
                        .param("year", String.valueOf(year))
                        .param("month", String.valueOf(month)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("요청이 성공적으로 처리되었습니다"))
                .andExpect(jsonPath("$.data.year").value(year))
                .andExpect(jsonPath("$.data.month").value(month))
                .andExpect(jsonPath("$.data.totalDiaries").value(12))
                .andExpect(jsonPath("$.data.totalChats").value(5))
                .andExpect(jsonPath("$.data.averageEmotionScore").value(0.68))
                .andExpect(jsonPath("$.data.emotionDistribution.평온").value(8))
                .andExpect(jsonPath("$.data.emotionDistribution.불안").value(4))
                .andExpect(jsonPath("$.data.dominantEmotion").value("평온"))
                .andExpect(jsonPath("$.data.weeklySummaries[0].weekNumber").value(1))
                .andExpect(jsonPath("$.data.weeklySummaries[0].diaryCount").value(4))
                .andExpect(jsonPath("$.data.weeklySummaries[0].dominantEmotion").value("평온"))
                .andExpect(jsonPath("$.data.weeklySummaries[1].weekNumber").value(2))
                .andExpect(jsonPath("$.data.comparisonWithLastMonth.scoreDiff").value(0.12))
                .andExpect(jsonPath("$.data.comparisonWithLastMonth.diaryCountDiff").value(2))
                .andExpect(jsonPath("$.data.comparisonWithLastMonth.trend").value("improving"));
    }
}
