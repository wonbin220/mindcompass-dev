// 파일: DiaryController.java
// 역할: 일기 API 컨트롤러
// 엔드포인트: /api/v1/diaries/**
// 화면: 일기 작성, 수정, 상세, 목록

package com.mindcompass.api.diary.controller;

import com.mindcompass.api.common.response.ApiResponse;
import com.mindcompass.api.common.security.CurrentUser;
import com.mindcompass.api.diary.dto.request.CreateDiaryRequest;
import com.mindcompass.api.diary.dto.request.UpdateDiaryRequest;
import com.mindcompass.api.diary.dto.response.DiaryListResponse;
import com.mindcompass.api.diary.dto.response.DiaryResponse;
import com.mindcompass.api.diary.service.DiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    /**
     * 일기 생성
     * POST /api/v1/diaries
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DiaryResponse>> createDiary(
            @CurrentUser Long userId,
            @Valid @RequestBody CreateDiaryRequest request) {
        DiaryResponse response = diaryService.createDiary(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "일기가 저장되었습니다"));
    }

    /**
     * 일기 단건 조회
     * GET /api/v1/diaries/{diaryId}
     */
    @GetMapping("/{diaryId}")
    public ResponseEntity<ApiResponse<DiaryResponse>> getDiary(
            @CurrentUser Long userId,
            @PathVariable Long diaryId) {
        DiaryResponse response = diaryService.getDiary(userId, diaryId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 일기 목록 조회 (페이징)
     * GET /api/v1/diaries
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<DiaryListResponse>>> getDiaries(
            @CurrentUser Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DiaryListResponse> response = diaryService.getDiaries(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 일기 수정
     * PATCH /api/v1/diaries/{diaryId}
     */
    @PatchMapping("/{diaryId}")
    public ResponseEntity<ApiResponse<DiaryResponse>> updateDiary(
            @CurrentUser Long userId,
            @PathVariable Long diaryId,
            @Valid @RequestBody UpdateDiaryRequest request) {
        DiaryResponse response = diaryService.updateDiary(userId, diaryId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "일기가 수정되었습니다"));
    }

    /**
     * 일기 삭제
     * DELETE /api/v1/diaries/{diaryId}
     */
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<ApiResponse<Void>> deleteDiary(
            @CurrentUser Long userId,
            @PathVariable Long diaryId) {
        diaryService.deleteDiary(userId, diaryId);
        return ResponseEntity.ok(ApiResponse.success("일기가 삭제되었습니다"));
    }

    /**
     * 일기 재분석 요청
     * POST /api/v1/diaries/{diaryId}/analyze
     */
    @PostMapping("/{diaryId}/analyze")
    public ResponseEntity<ApiResponse<DiaryResponse>> analyzeDiary(
            @CurrentUser Long userId,
            @PathVariable Long diaryId) {
        DiaryResponse response = diaryService.analyzeDiary(userId, diaryId);
        return ResponseEntity.ok(ApiResponse.success(response, "분석이 요청되었습니다"));
    }
}
