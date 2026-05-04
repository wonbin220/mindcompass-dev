// 파일: ReportServiceTest.java
// 역할: 주간/월간 리포트 서비스 테스트
// 검증: 감정 분포, 일별/주별 요약, 채팅 수, 전월 비교

package com.mindcompass.api.report.service;

import com.mindcompass.api.chat.repository.ChatSessionRepository;
import com.mindcompass.api.diary.domain.Diary;
import com.mindcompass.api.diary.repository.DiaryQueryRepository;
import com.mindcompass.api.diary.repository.DiaryRepository;
import com.mindcompass.api.report.dto.response.MonthlyReportResponse;
import com.mindcompass.api.report.dto.response.WeeklyReportResponse;
import com.mindcompass.api.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;

    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private DiaryQueryRepository diaryQueryRepository;

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Test
    @DisplayName("주간 리포트 생성 성공 - 감정 분포, 일별 추이, 일기/채팅 수를 계산한다")
    void getWeeklyReport_success() {
        // given
        Long userId = 1L;
        LocalDate targetDate = LocalDate.of(2026, 4, 8);
        LocalDate startDate = LocalDate.of(2026, 4, 6);
        LocalDate endDate = LocalDate.of(2026, 4, 12);
        User user = createUser(userId);

        List<Diary> diaries = List.of(
                createDiary(user, LocalDate.of(2026, 4, 10), "기쁨", 4),
                createDiary(user, LocalDate.of(2026, 4, 8), "불안", 2),
                createDiary(user, LocalDate.of(2026, 4, 7), "기쁨", 3)
        );

        given(diaryRepository.findByUserIdAndWrittenAtBetweenAndDeletedAtIsNullOrderByWrittenAtDesc(
                userId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()))
                .willReturn(diaries);
        given(chatSessionRepository.countByUserIdAndCreatedAtBetween(
                userId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        )).willReturn(3L);

        // when
        WeeklyReportResponse response = reportService.getWeeklyReport(userId, targetDate);

        // then
        assertThat(response.getStartDate()).isEqualTo(startDate);
        assertThat(response.getEndDate()).isEqualTo(endDate);
        assertThat(response.getTotalDiaries()).isEqualTo(3);
        assertThat(response.getTotalChats()).isEqualTo(3);
        assertThat(response.getAverageEmotionScore()).isEqualTo(3.0);
        assertThat(response.getEmotionDistribution())
                .containsEntry("기쁨", 2L)
                .containsEntry("불안", 1L);
        assertThat(response.getDominantEmotion()).isEqualTo("기쁨");
        assertThat(response.getDailyTrends()).hasSize(7);
        assertThat(response.getDailyTrends().get(0).getDate()).isEqualTo(startDate);
        assertThat(response.getDailyTrends().get(0).getEmotion()).isNull();
        assertThat(response.getDailyTrends().get(0).getScore()).isNull();
        assertThat(response.getDailyTrends().get(0).getHasDiary()).isFalse();
        assertThat(response.getDailyTrends().get(1).getDate()).isEqualTo(LocalDate.of(2026, 4, 7));
        assertThat(response.getDailyTrends().get(1).getEmotion()).isEqualTo("기쁨");
        assertThat(response.getDailyTrends().get(1).getScore()).isEqualTo(3.0);
        assertThat(response.getDailyTrends().get(1).getHasDiary()).isTrue();
        assertThat(response.getDailyTrends().get(2).getDate()).isEqualTo(LocalDate.of(2026, 4, 8));
        assertThat(response.getDailyTrends().get(2).getEmotion()).isEqualTo("불안");
        assertThat(response.getDailyTrends().get(2).getScore()).isEqualTo(2.0);
        assertThat(response.getDailyTrends().get(2).getHasDiary()).isTrue();
        assertThat(response.getDailyTrends().get(3).getDate()).isEqualTo(LocalDate.of(2026, 4, 9));
        assertThat(response.getDailyTrends().get(3).getEmotion()).isNull();
        assertThat(response.getDailyTrends().get(3).getScore()).isNull();
        assertThat(response.getDailyTrends().get(3).getHasDiary()).isFalse();
        assertThat(response.getDailyTrends().get(4).getDate()).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(response.getDailyTrends().get(4).getEmotion()).isEqualTo("기쁨");
        assertThat(response.getDailyTrends().get(4).getScore()).isEqualTo(4.0);
        assertThat(response.getDailyTrends().get(4).getHasDiary()).isTrue();
        assertThat(response.getAiSummary()).isNull();

        verify(diaryRepository).findByUserIdAndWrittenAtBetweenAndDeletedAtIsNullOrderByWrittenAtDesc(
                userId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        verify(chatSessionRepository).countByUserIdAndCreatedAtBetween(
                userId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );
    }

    @Test
    @DisplayName("월간 리포트 생성 성공 - 주별 요약과 전월 비교를 계산한다")
    void getMonthlyReport_success() {
        // given
        Long userId = 1L;
        int year = 2026;
        int month = 1;
        LocalDate currentStart = LocalDate.of(2026, 1, 1);
        LocalDate currentEnd = LocalDate.of(2026, 1, 31);
        LocalDate lastStart = LocalDate.of(2025, 12, 1);
        LocalDate lastEnd = LocalDate.of(2025, 12, 31);
        User user = createUser(userId);

        List<Diary> currentMonthDiaries = List.of(
                createDiary(user, LocalDate.of(2026, 1, 30), "기쁨", 5),
                createDiary(user, LocalDate.of(2026, 1, 15), "슬픔", 3),
                createDiary(user, LocalDate.of(2026, 1, 9), "기쁨", 4),
                createDiary(user, LocalDate.of(2026, 1, 2), "불안", 4)
        );
        List<Diary> lastMonthDiaries = List.of(
                createDiary(user, LocalDate.of(2025, 12, 20), "불안", 2),
                createDiary(user, LocalDate.of(2025, 12, 5), "슬픔", 2)
        );

        given(diaryRepository.findByUserIdAndWrittenAtBetweenAndDeletedAtIsNullOrderByWrittenAtDesc(
                userId, currentStart.atStartOfDay(), currentEnd.plusDays(1).atStartOfDay()))
                .willReturn(currentMonthDiaries);
        given(diaryRepository.findByUserIdAndWrittenAtBetweenAndDeletedAtIsNullOrderByWrittenAtDesc(
                userId, lastStart.atStartOfDay(), lastEnd.plusDays(1).atStartOfDay()))
                .willReturn(lastMonthDiaries);
        given(chatSessionRepository.countByUserIdAndCreatedAtBetween(
                userId,
                currentStart.atStartOfDay(),
                currentEnd.plusDays(1).atStartOfDay()
        )).willReturn(5L);

        // when
        MonthlyReportResponse response = reportService.getMonthlyReport(userId, year, month);

        // then
        assertThat(response.getYear()).isEqualTo(2026);
        assertThat(response.getMonth()).isEqualTo(1);
        assertThat(response.getTotalDiaries()).isEqualTo(4);
        assertThat(response.getTotalChats()).isEqualTo(5);
        assertThat(response.getAverageEmotionScore()).isEqualTo(4.0);
        assertThat(response.getEmotionDistribution())
                .containsEntry("기쁨", 2L)
                .containsEntry("슬픔", 1L)
                .containsEntry("불안", 1L);
        assertThat(response.getDominantEmotion()).isEqualTo("기쁨");
        assertThat(response.getWeeklySummaries()).hasSize(5);
        assertThat(response.getWeeklySummaries().get(0).getWeekNumber()).isEqualTo(1);
        assertThat(response.getWeeklySummaries().get(0).getDiaryCount()).isEqualTo(1);
        assertThat(response.getWeeklySummaries().get(0).getDominantEmotion()).isEqualTo("불안");
        assertThat(response.getWeeklySummaries().get(0).getAverageScore()).isEqualTo(4.0);
        assertThat(response.getWeeklySummaries().get(1).getWeekNumber()).isEqualTo(2);
        assertThat(response.getWeeklySummaries().get(1).getDiaryCount()).isEqualTo(1);
        assertThat(response.getWeeklySummaries().get(1).getDominantEmotion()).isEqualTo("기쁨");
        assertThat(response.getWeeklySummaries().get(1).getAverageScore()).isEqualTo(4.0);
        assertThat(response.getWeeklySummaries().get(2).getWeekNumber()).isEqualTo(3);
        assertThat(response.getWeeklySummaries().get(2).getDiaryCount()).isEqualTo(1);
        assertThat(response.getWeeklySummaries().get(2).getDominantEmotion()).isEqualTo("슬픔");
        assertThat(response.getWeeklySummaries().get(2).getAverageScore()).isEqualTo(3.0);
        assertThat(response.getWeeklySummaries().get(3).getWeekNumber()).isEqualTo(4);
        assertThat(response.getWeeklySummaries().get(3).getDiaryCount()).isEqualTo(0);
        assertThat(response.getWeeklySummaries().get(3).getDominantEmotion()).isNull();
        assertThat(response.getWeeklySummaries().get(3).getAverageScore()).isEqualTo(0.0);
        assertThat(response.getWeeklySummaries().get(4).getWeekNumber()).isEqualTo(5);
        assertThat(response.getWeeklySummaries().get(4).getDiaryCount()).isEqualTo(1);
        assertThat(response.getWeeklySummaries().get(4).getDominantEmotion()).isEqualTo("기쁨");
        assertThat(response.getWeeklySummaries().get(4).getAverageScore()).isEqualTo(5.0);
        assertThat(response.getComparisonWithLastMonth().getScoreDiff()).isEqualTo(2.0);
        assertThat(response.getComparisonWithLastMonth().getDiaryCountDiff()).isEqualTo(2);
        assertThat(response.getComparisonWithLastMonth().getTrend()).isEqualTo("improving");
        assertThat(response.getAiInsight()).isNull();

        verify(diaryRepository).findByUserIdAndWrittenAtBetweenAndDeletedAtIsNullOrderByWrittenAtDesc(
                userId, currentStart.atStartOfDay(), currentEnd.plusDays(1).atStartOfDay());
        verify(diaryRepository).findByUserIdAndWrittenAtBetweenAndDeletedAtIsNullOrderByWrittenAtDesc(
                userId, lastStart.atStartOfDay(), lastEnd.plusDays(1).atStartOfDay());
        verify(chatSessionRepository).countByUserIdAndCreatedAtBetween(
                userId,
                currentStart.atStartOfDay(),
                currentEnd.plusDays(1).atStartOfDay()
        );
    }

    private User createUser(Long userId) {
        User user = User.builder()
                .email("report@test.com")
                .passwordHash("encodedPassword")
                .nickname("report-user")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private Diary createDiary(User user, LocalDate diaryDate, String emotion, int emotionIntensity) {
        Diary diary = Diary.builder()
                .user(user)
                .title("리포트 테스트 일기")
                .content("테스트용 감정 기록")
                .writtenAt(diaryDate.atTime(21, 0))
                .primaryEmotion(emotion)
                .emotionIntensity(emotionIntensity)
                .build();
        ReflectionTestUtils.setField(diary, "createdAt", LocalDateTime.of(2026, 4, 8, 17, 20));
        ReflectionTestUtils.setField(diary, "updatedAt", LocalDateTime.of(2026, 4, 8, 17, 20));
        return diary;
    }
}
