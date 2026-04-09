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

        Diary happyDiary = createDiary(1L, LocalDate.of(2026, 4, 8), "기쁨", 0.91);
        Diary calmDiary = createDiary(2L, LocalDate.of(2026, 4, 3), "평온", 0.72);
        Diary anotherHappyDiary = createDiary(3L, LocalDate.of(2026, 4, 1), "기쁨", 0.65);

        given(diaryRepository.findByUserIdAndDiaryDateBetweenOrderByDiaryDateDesc(userId, startDate, endDate))
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
                    assertThat(day.getDiaryId()).isEqualTo(1L);
                    assertThat(day.getPrimaryEmotion()).isEqualTo("기쁨");
                    assertThat(day.getEmotionScore()).isEqualTo(0.91);
                });

        assertThat(response.getDays())
                .filteredOn(day -> day.getDate().isEqual(LocalDate.of(2026, 4, 2)))
                .singleElement()
                .satisfies(day -> {
                    assertThat(day.getHasDiary()).isFalse();
                    assertThat(day.getDiaryId()).isNull();
                    assertThat(day.getPrimaryEmotion()).isNull();
                    assertThat(day.getEmotionScore()).isNull();
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

        given(diaryRepository.findByUserIdAndDiaryDateBetweenOrderByDiaryDateDesc(userId, startDate, endDate))
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
        Diary diary = createDiary(1L, date, "기쁨", 0.88);

        given(diaryRepository.findByUserIdAndDiaryDate(userId, date))
                .willReturn(Optional.of(diary));

        // when
        DiaryListResponse response = calendarService.getDiaryByDate(userId, date);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("테스트 일기 1");
        assertThat(response.getDiaryDate()).isEqualTo(date);
        assertThat(response.getPrimaryEmotion()).isEqualTo("기쁨");
        assertThat(response.getEmotionScore()).isEqualTo(0.88);
        assertThat(response.getIsAnalyzed()).isTrue();
    }

    @Test
    @DisplayName("특정 날짜 일기 조회 성공 - 일기 없는 날")
    void getDiaryByDate_success_withoutDiary() {
        // given
        Long userId = 1L;
        LocalDate date = LocalDate.of(2026, 4, 9);

        given(diaryRepository.findByUserIdAndDiaryDate(userId, date))
                .willReturn(Optional.empty());

        // when
        DiaryListResponse response = calendarService.getDiaryByDate(userId, date);

        // then
        assertThat(response).isNull();
    }

    @Test
    @DisplayName("감정별 일기 목록 조회 성공")
    void getDiariesByEmotion_success() {
        // given
        Long userId = 1L;
        String emotion = "기쁨";
        int limit = 20;

        Diary firstDiary = createDiary(1L, LocalDate.of(2026, 4, 8), emotion, 0.91);
        Diary secondDiary = createDiary(2L, LocalDate.of(2026, 4, 6), emotion, 0.74);

        given(diaryQueryRepository.findByUserIdAndEmotion(userId, emotion, limit))
                .willReturn(List.of(firstDiary, secondDiary));

        // when
        List<DiaryListResponse> response = calendarService.getDiariesByEmotion(userId, emotion, limit);

        // then
        assertThat(response).hasSize(2);
        assertThat(response.get(0).getId()).isEqualTo(1L);
        assertThat(response.get(0).getPrimaryEmotion()).isEqualTo("기쁨");
        assertThat(response.get(1).getId()).isEqualTo(2L);
        assertThat(response.get(1).getDiaryDate()).isEqualTo(LocalDate.of(2026, 4, 6));
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

    private Diary createDiary(Long diaryId, LocalDate diaryDate, String primaryEmotion, Double emotionScore) {
        User user = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .name("테스트 사용자")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        Diary diary = Diary.builder()
                .user(user)
                .title("테스트 일기 " + diaryId)
                .content("테스트 내용 " + diaryId)
                .diaryDate(diaryDate)
                .build();

        ReflectionTestUtils.setField(diary, "id", diaryId);
        ReflectionTestUtils.setField(diary, "primaryEmotion", primaryEmotion);
        ReflectionTestUtils.setField(diary, "emotionScore", emotionScore);
        ReflectionTestUtils.setField(diary, "isAnalyzed", true);

        return diary;
    }
}
