// 파일: MonthlyReportResponse.java
// 역할: 월간 리포트 응답 DTO
// 화면: 월간 감정 리포트 페이지

package com.mindcompass.api.report.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class MonthlyReportResponse {

    private final int year;
    private final int month;

    // 기본 통계
    private final int totalDiaries;
    private final int totalChats;
    private final Double averageEmotionScore;

    // 감정 분포
    private final Map<String, Long> emotionDistribution;
    private final String dominantEmotion;

    // 주별 요약
    private final List<WeeklySummary> weeklySummaries;

    // 전월 대비 변화
    private final EmotionComparison comparisonWithLastMonth;

    // AI 생성 인사이트 (선택적)
    private final String aiInsight;

    @Getter
    @Builder
    public static class WeeklySummary {
        private final int weekNumber;
        private final int diaryCount;
        private final String dominantEmotion;
        private final Double averageScore;
    }

    @Getter
    @Builder
    public static class EmotionComparison {
        private final Double scoreDiff;      // 감정 점수 차이
        private final int diaryCountDiff;    // 일기 수 차이
        private final String trend;          // "improving", "stable", "declining"
    }
}
