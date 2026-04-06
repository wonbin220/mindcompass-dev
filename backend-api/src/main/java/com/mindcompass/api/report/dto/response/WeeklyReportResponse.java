// 파일: WeeklyReportResponse.java
// 역할: 주간 리포트 응답 DTO
// 화면: 주간 감정 리포트 페이지

package com.mindcompass.api.report.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class WeeklyReportResponse {

    private final LocalDate startDate;
    private final LocalDate endDate;

    // 기본 통계
    private final int totalDiaries;
    private final int totalChats;
    private final Double averageEmotionScore;

    // 감정 분포
    private final Map<String, Long> emotionDistribution;
    private final String dominantEmotion;  // 가장 많이 나타난 감정

    // 일별 감정 추이
    private final List<DailyEmotionTrend> dailyTrends;

    // AI 생성 요약 (선택적)
    private final String aiSummary;

    @Getter
    @Builder
    public static class DailyEmotionTrend {
        private final LocalDate date;
        private final String emotion;
        private final Double score;
        private final Boolean hasDiary;
    }
}
