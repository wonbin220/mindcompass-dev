// 파일: ReportService.java
// 역할: 리포트 비즈니스 로직
// 흐름: ReportController -> ReportService -> DiaryRepository, ChatRepository

package com.mindcompass.api.report.service;

import com.mindcompass.api.chat.repository.ChatMessageRepository;
import com.mindcompass.api.chat.repository.ChatSessionRepository;
import com.mindcompass.api.diary.domain.Diary;
import com.mindcompass.api.diary.repository.DiaryQueryRepository;
import com.mindcompass.api.diary.repository.DiaryRepository;
import com.mindcompass.api.report.dto.response.MonthlyReportResponse;
import com.mindcompass.api.report.dto.response.WeeklyReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final DiaryRepository diaryRepository;
    private final DiaryQueryRepository diaryQueryRepository;
    private final ChatSessionRepository chatSessionRepository;

    /**
     * 주간 리포트 생성
     */
    public WeeklyReportResponse getWeeklyReport(Long userId, LocalDate date) {
        // 해당 주의 시작일(월)과 종료일(일) 계산
        LocalDate startDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endDate = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        // 일기 조회
        List<Diary> diaries = diaryRepository
                .findByUserIdAndDiaryDateBetweenOrderByDiaryDateDesc(userId, startDate, endDate);

        // 감정 분포 계산
        Map<String, Long> emotionDistribution = diaries.stream()
                .filter(d -> d.getPrimaryEmotion() != null)
                .collect(Collectors.groupingBy(Diary::getPrimaryEmotion, Collectors.counting()));

        // 지배적 감정 찾기
        String dominantEmotion = emotionDistribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // 평균 감정 점수
        Double avgScore = diaries.stream()
                .filter(d -> d.getEmotionScore() != null)
                .mapToDouble(Diary::getEmotionScore)
                .average()
                .orElse(0.0);

        // 일별 추이 생성
        Map<LocalDate, Diary> diaryMap = diaries.stream()
                .collect(Collectors.toMap(Diary::getDiaryDate, d -> d, (a, b) -> a));

        List<WeeklyReportResponse.DailyEmotionTrend> dailyTrends = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            Diary diary = diaryMap.get(d);
            dailyTrends.add(WeeklyReportResponse.DailyEmotionTrend.builder()
                    .date(d)
                    .emotion(diary != null ? diary.getPrimaryEmotion() : null)
                    .score(diary != null ? diary.getEmotionScore() : null)
                    .hasDiary(diary != null)
                    .build());
        }

        // TODO: 채팅 수 조회 추가
        int totalChats = 0;

        return WeeklyReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalDiaries(diaries.size())
                .totalChats(totalChats)
                .averageEmotionScore(avgScore)
                .emotionDistribution(emotionDistribution)
                .dominantEmotion(dominantEmotion)
                .dailyTrends(dailyTrends)
                .aiSummary(null) // TODO: AI 요약 생성 연동
                .build();
    }

    /**
     * 월간 리포트 생성
     */
    public MonthlyReportResponse getMonthlyReport(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 일기 조회
        List<Diary> diaries = diaryRepository
                .findByUserIdAndDiaryDateBetweenOrderByDiaryDateDesc(userId, startDate, endDate);

        // 감정 분포
        Map<String, Long> emotionDistribution = diaries.stream()
                .filter(d -> d.getPrimaryEmotion() != null)
                .collect(Collectors.groupingBy(Diary::getPrimaryEmotion, Collectors.counting()));

        String dominantEmotion = emotionDistribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        Double avgScore = diaries.stream()
                .filter(d -> d.getEmotionScore() != null)
                .mapToDouble(Diary::getEmotionScore)
                .average()
                .orElse(0.0);

        // 주별 요약 생성
        List<MonthlyReportResponse.WeeklySummary> weeklySummaries = generateWeeklySummaries(diaries, yearMonth);

        // 전월 비교
        MonthlyReportResponse.EmotionComparison comparison = compareWithLastMonth(userId, yearMonth, avgScore, diaries.size());

        return MonthlyReportResponse.builder()
                .year(year)
                .month(month)
                .totalDiaries(diaries.size())
                .totalChats(0) // TODO: 채팅 수 조회
                .averageEmotionScore(avgScore)
                .emotionDistribution(emotionDistribution)
                .dominantEmotion(dominantEmotion)
                .weeklySummaries(weeklySummaries)
                .comparisonWithLastMonth(comparison)
                .aiInsight(null) // TODO: AI 인사이트 생성
                .build();
    }

    private List<MonthlyReportResponse.WeeklySummary> generateWeeklySummaries(
            List<Diary> diaries, YearMonth yearMonth) {
        // TODO: 실제 주별 그룹핑 로직 구현
        return List.of();
    }

    private MonthlyReportResponse.EmotionComparison compareWithLastMonth(
            Long userId, YearMonth currentMonth, Double currentAvgScore, int currentDiaryCount) {

        YearMonth lastMonth = currentMonth.minusMonths(1);
        LocalDate lastStart = lastMonth.atDay(1);
        LocalDate lastEnd = lastMonth.atEndOfMonth();

        List<Diary> lastMonthDiaries = diaryRepository
                .findByUserIdAndDiaryDateBetweenOrderByDiaryDateDesc(userId, lastStart, lastEnd);

        Double lastAvgScore = lastMonthDiaries.stream()
                .filter(d -> d.getEmotionScore() != null)
                .mapToDouble(Diary::getEmotionScore)
                .average()
                .orElse(0.0);

        double scoreDiff = currentAvgScore - lastAvgScore;
        int countDiff = currentDiaryCount - lastMonthDiaries.size();

        String trend = scoreDiff > 0.1 ? "improving" :
                      scoreDiff < -0.1 ? "declining" : "stable";

        return MonthlyReportResponse.EmotionComparison.builder()
                .scoreDiff(scoreDiff)
                .diaryCountDiff(countDiff)
                .trend(trend)
                .build();
    }
}
