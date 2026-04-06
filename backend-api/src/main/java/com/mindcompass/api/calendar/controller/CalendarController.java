// 파일: CalendarController.java
// 역할: 캘린더 API 컨트롤러
// 엔드포인트: /api/v1/calendar/**
// 화면: 캘린더 뷰

package com.mindcompass.api.calendar.controller;

import com.mindcompass.api.calendar.dto.response.CalendarMonthResponse;
import com.mindcompass.api.calendar.service.CalendarService;
import com.mindcompass.api.common.response.ApiResponse;
import com.mindcompass.api.common.security.CurrentUser;
import com.mindcompass.api.diary.dto.response.DiaryListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    /**
     * 월별 캘린더 조회
     * GET /api/v1/calendar?year=2024&month=1
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CalendarMonthResponse>> getMonthCalendar(
            @CurrentUser Long userId,
            @RequestParam int year,
            @RequestParam int month) {
        CalendarMonthResponse response = calendarService.getMonthCalendar(userId, year, month);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 특정 날짜의 일기 조회
     * GET /api/v1/calendar/{date}
     */
    @GetMapping("/{date}")
    public ResponseEntity<ApiResponse<DiaryListResponse>> getDiaryByDate(
            @CurrentUser Long userId,
            @PathVariable LocalDate date) {
        DiaryListResponse response = calendarService.getDiaryByDate(userId, date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 감정별 일기 목록 조회
     * GET /api/v1/calendar/emotion/{emotion}
     */
    @GetMapping("/emotion/{emotion}")
    public ResponseEntity<ApiResponse<List<DiaryListResponse>>> getDiariesByEmotion(
            @CurrentUser Long userId,
            @PathVariable String emotion,
            @RequestParam(defaultValue = "20") int limit) {
        List<DiaryListResponse> response = calendarService.getDiariesByEmotion(userId, emotion, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
