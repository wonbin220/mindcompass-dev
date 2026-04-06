// 파일: ReportController.java
// 역할: 리포트 API 컨트롤러
// 엔드포인트: /api/v1/reports/**
// 화면: 주간/월간 리포트 페이지

package com.mindcompass.api.report.controller;

import com.mindcompass.api.common.response.ApiResponse;
import com.mindcompass.api.common.security.CurrentUser;
import com.mindcompass.api.report.dto.response.MonthlyReportResponse;
import com.mindcompass.api.report.dto.response.WeeklyReportResponse;
import com.mindcompass.api.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 주간 리포트 조회
     * GET /api/v1/reports/weekly?date=2024-01-15
     * date가 포함된 주의 리포트를 반환
     */
    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<WeeklyReportResponse>> getWeeklyReport(
            @CurrentUser Long userId,
            @RequestParam(required = false) LocalDate date) {

        // date가 없으면 이번 주
        LocalDate targetDate = date != null ? date : LocalDate.now();
        WeeklyReportResponse response = reportService.getWeeklyReport(userId, targetDate);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 월간 리포트 조회
     * GET /api/v1/reports/monthly?year=2024&month=1
     */
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<MonthlyReportResponse>> getMonthlyReport(
            @CurrentUser Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        // year/month가 없으면 이번 달
        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();
        int targetMonth = month != null ? month : now.getMonthValue();

        MonthlyReportResponse response = reportService.getMonthlyReport(userId, targetYear, targetMonth);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
