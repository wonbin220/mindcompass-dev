package com.mindcompass.api.calendar.service;

import com.mindcompass.api.calendar.dto.response.CalendarMonthResponse;
import com.mindcompass.api.diary.domain.Diary;
import com.mindcompass.api.diary.dto.response.DiaryListResponse;
import com.mindcompass.api.diary.repository.DiaryQueryRepository;
import com.mindcompass.api.diary.repository.DiaryRepository;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @InjectMocks
    private CalendarService calendarService;

    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private DiaryQueryRepository diaryQueryRepository;

    @Test
    @DisplayName("월별 캘린더 조회 성공 - 일기 있는 달")
    void getMonthCalendar_success_withDiaries() {
        // given
        Long userId = 1L;
        int year = 2026;
        int month = 4;
        LocalDate startDate = LocalDate.of(2026, 4, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 30);

        Diary happyDiary = createDiary(1L, LocalDate.of(2026, 4, 8), "기쁨", 5);
        Diary calmDiary = createDiary(2L, LocalDate.of(2026, 4, 3), "평온", 4);
        Diary anotherHappyDiary = createDiary(3L, LocalDate.of(2026, 4, 1), "기쁨", 3);

        given(diaryRepository.findByUserIdAndWrittenAtBetweenAndDeletedAtIsNullOrderByWrittenAtDesc(
                userId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()))
                .willReturn(List.of(happyDiary, calmDiary, anotherHappyDiary));

        // when
        CalendarMonthResponse response = calendarService.getMonthCalendar(userId, year, month);

        // then
        assertThat(response.getYear()).isEqualTo(2026);
        assertThat(response.getMonth()).isEqualTo(4);
        assertThat(response.getDays()).hasSize(30);
        assertThat(response.getTotalDiaries()).isEqualTo(3);
        assertThat(response.getEmotionSummary()).isEqualTo(Map.of("기쁨", 2L, "평온", 1L));

        assertThat(response.getDays())
                .filteredOn(day -> day.getDate().isEqual(LocalDate.of(2026, 4, 8)))
                .singleElement()
                .satisfies(day -> {
                    assertThat(day.getHasDiary()).isTrue();
                    assertThat(day.getDiaries()).hasSize(1);
                    assertThat(day.getDiaries().get(0).getId()).isEqualTo(1L);
                    assertThat(day.getDiaries().get(0).getPrimaryEmotion()).isEqualTo("기쁨");
                    // emotionIntensity는 DiaryBriefInfo에 없으므로 검증 제거
                });

        assertThat(response.getDays())
                .filteredOn(day -> day.getDate().isEqual(LocalDate.of(2026, 4, 2)))
                .singleElement()
                .satisfies(day -> {
                    assertThat(day.getHasDiary()).isFalse();
                    assertThat(day.getDiaries()).isEmpty();
                });
    }

    @Test
    @DisplayName("월별 캘린더 조회 성공 - 일기 없는 달")
    void getMonthCalendar_success_withoutDiaries() {
        // given
        Long userId = 1L;
        int year = 2026;
        int month = 2;
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 28);

        given(diaryRepository.findByUserIdAndWrittenAtBetweenAndDeletedAtIsNullOrderByWrittenAtDesc(
                userId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()))
                .willReturn(List.of());

        // when
        CalendarMonthResponse response = calendarService.getMonthCalendar(userId, year, month);

        // then
        assertThat(response.getYear()).isEqualTo(2026);
        assertThat(response.getMonth()).isEqualTo(2);
        assertThat(response.getDays()).hasSize(28);
        assertThat(response.getTotalDiaries()).isZero();
        assertThat(response.getEmotionSummary()).isEmpty();
        assertThat(response.getDays()).allSatisfy(day -> assertThat(day.getHasDiary()).isFalse());
    }

    @Test
    @DisplayName("특정 날짜 일기 조회 성공 - 일기 있는 날")
    void getDiaryByDate_success_withDiary() {
        // given
        Long userId = 1L;
        LocalDate date = LocalDate.of(2026, 4, 8);
        Diary diary = createDiary(1L, date, "기쁨", 4);

        // findFirst → findBy (List 반환으로 바뀜)
        given(diaryRepository.findByUserIdAndWrittenAtBetweenAndDeletedAtIsNullOrderByWrittenAtDesc(
                userId, date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .willReturn(List.of(diary));

        // when
        List<DiaryListResponse> response = calendarService.getDiaryByDate(userId, date);

        // then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getId()).isEqualTo(1L);
        assertThat(response.get(0).getTitle()).isEqualTo("테스트 일기 1");
        assertThat(response.get(0).getPrimaryEmotion()).isEqualTo("기쁨");
        assertThat(response.get(0).getEmotionIntensity()).isEqualTo(4);
    }


    @Test
    @DisplayName("특정 날짜 일기 조회 성공 - 일기 없는 날")
    void getDiaryByDate_success_withoutDiary() {
        // given
        Long userId = 1L;
        LocalDate date = LocalDate.of(2026, 4, 9);

        given(diaryRepository.findByUserIdAndWrittenAtBetweenAndDeletedAtIsNullOrderByWrittenAtDesc(
                userId, date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .willReturn(List.of());

        // when
        List<DiaryListResponse> response = calendarService.getDiaryByDate(userId, date);

        // then
        assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("감정별 일기 목록 조회 성공")
    void getDiariesByEmotion_success() {
        // given
        Long userId = 1L;
        String emotion = "기쁨";
        int limit = 20;

        Diary firstDiary = createDiary(1L, LocalDate.of(2026, 4, 8), emotion, 5);
        Diary secondDiary = createDiary(2L, LocalDate.of(2026, 4, 6), emotion, 3);

        given(diaryQueryRepository.findByUserIdAndEmotion(userId, emotion, limit))
                .willReturn(List.of(firstDiary, secondDiary));

        // when
        List<DiaryListResponse> response = calendarService.getDiariesByEmotion(userId, emotion, limit);

        // then
        assertThat(response).hasSize(2);
        assertThat(response.get(0).getId()).isEqualTo(1L);
        assertThat(response.get(0).getPrimaryEmotion()).isEqualTo("기쁨");
        assertThat(response.get(1).getId()).isEqualTo(2L);
        assertThat(response.get(1).getWrittenAt()).isEqualTo(LocalDate.of(2026, 4, 6).atTime(21, 0));
    }

    @Test
    @DisplayName("감정별 일기 목록 조회 성공 - 해당 감정 없음")
    void getDiariesByEmotion_success_emptyResult() {
        // given
        Long userId = 1L;
        String emotion = "불안";
        int limit = 20;

        given(diaryQueryRepository.findByUserIdAndEmotion(userId, emotion, limit))
                .willReturn(List.of());

        // when
        List<DiaryListResponse> response = calendarService.getDiariesByEmotion(userId, emotion, limit);

        // then
        assertThat(response).isEmpty();
    }

    private Diary createDiary(Long diaryId, LocalDate diaryDate, String primaryEmotion, Integer emotionIntensity) {
        User user = User.builder()
                .email("test@test.com")
                .passwordHash("encodedPassword")
                .nickname("테스트 사용자")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        Diary diary = Diary.builder()
                .user(user)
                .title("테스트 일기 " + diaryId)
                .content("테스트 내용 " + diaryId)
                .writtenAt(LocalDateTime.of(diaryDate.getYear(), diaryDate.getMonth(), diaryDate.getDayOfMonth(), 21, 0))
                .build();

        ReflectionTestUtils.setField(diary, "id", diaryId);
        ReflectionTestUtils.setField(diary, "primaryEmotion", primaryEmotion);
        ReflectionTestUtils.setField(diary, "emotionIntensity", emotionIntensity);

        return diary;
    }
}
