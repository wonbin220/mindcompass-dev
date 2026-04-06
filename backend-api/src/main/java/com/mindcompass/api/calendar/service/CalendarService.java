// 파일: CalendarService.java
// 역할: 캘린더 비즈니스 로직
// 흐름: CalendarController -> CalendarService -> DiaryRepository, DiaryQueryRepository

package com.mindcompass.api.calendar.service;

import com.mindcompass.api.calendar.dto.response.CalendarDayResponse;
import com.mindcompass.api.calendar.dto.response.CalendarMonthResponse;
import com.mindcompass.api.diary.domain.Diary;
import com.mindcompass.api.diary.dto.response.DiaryListResponse;
import com.mindcompass.api.diary.repository.DiaryQueryRepository;
import com.mindcompass.api.diary.repository.DiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private final DiaryRepository diaryRepository;
    private final DiaryQueryRepository diaryQueryRepository;

    /**
     * 월별 캘린더 조회
     */
    public CalendarMonthResponse getMonthCalendar(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 해당 월의 일기 목록 조회
        List<Diary> diaries = diaryRepository
                .findByUserIdAndDiaryDateBetweenOrderByDiaryDateDesc(userId, startDate, endDate);

        // 날짜별 일기 매핑
        Map<LocalDate, Diary> diaryMap = diaries.stream()
                .collect(Collectors.toMap(Diary::getDiaryDate, d -> d, (a, b) -> a));

        // 캘린더 데이 생성
        List<CalendarDayResponse> days = new ArrayList<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            Diary diary = diaryMap.get(date);

            if (diary != null) {
                days.add(CalendarDayResponse.of(
                        date,
                        diary.getId(),
                        diary.getPrimaryEmotion(),
                        diary.getEmotionScore()
                ));
            } else {
                days.add(CalendarDayResponse.empty(date));
            }
        }

        // 감정 통계
        Map<String, Long> emotionSummary = diaries.stream()
                .filter(d -> d.getPrimaryEmotion() != null)
                .collect(Collectors.groupingBy(Diary::getPrimaryEmotion, Collectors.counting()));

        return CalendarMonthResponse.of(year, month, days, emotionSummary);
    }

    /**
     * 특정 날짜의 일기 조회
     */
    public DiaryListResponse getDiaryByDate(Long userId, LocalDate date) {
        return diaryRepository.findByUserIdAndDiaryDate(userId, date)
                .map(DiaryListResponse::from)
                .orElse(null);
    }

    /**
     * 감정별 일기 목록 조회
     */
    public List<DiaryListResponse> getDiariesByEmotion(Long userId, String emotion, int limit) {
        return diaryQueryRepository.findByUserIdAndEmotion(userId, emotion, limit)
                .stream()
                .map(DiaryListResponse::from)
                .toList();
    }
}
