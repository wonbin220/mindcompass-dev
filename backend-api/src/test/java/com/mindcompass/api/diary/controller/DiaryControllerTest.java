package com.mindcompass.api.diary.controller;

import com.mindcompass.api.common.security.JwtTokenProvider;
import com.mindcompass.api.diary.dto.request.CreateDiaryRequest;
import com.mindcompass.api.diary.dto.request.UpdateDiaryRequest;
import com.mindcompass.api.diary.dto.response.DiaryListResponse;
import com.mindcompass.api.diary.dto.response.DiaryResponse;
import com.mindcompass.api.diary.service.DiaryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DiaryController.class)
@AutoConfigureMockMvc(addFilters = false)
class DiaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiaryService diaryService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("POST /api/v1/diaries - 일기 생성 성공 (201)")
    void createDiary_success() throws Exception {
        // given
        DiaryResponse response = createDiaryResponse(1L);
        given(diaryService.createDiary(isNull(), any(CreateDiaryRequest.class)))
                .willReturn(response);

        String requestJson = """
                {"title":"오늘의 일기","content":"오늘은 좋은 하루였다","diaryDate":"2026-04-08"}
                """;

        // when & then
        mockMvc.perform(post("/api/v1/diaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("일기가 저장되었습니다"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("오늘의 일기"))
                .andExpect(jsonPath("$.data.content").value("오늘은 좋은 하루였다"))
                .andExpect(jsonPath("$.data.primaryEmotion").value("기쁨"))
                .andExpect(jsonPath("$.data.isAnalyzed").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/diaries/{id} - 일기 단건 조회 성공")
    void getDiary_success() throws Exception {
        // given
        DiaryResponse response = createDiaryResponse(1L);
        given(diaryService.getDiary(isNull(), eq(1L)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/diaries/{diaryId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("오늘의 일기"))
                .andExpect(jsonPath("$.data.content").value("오늘은 좋은 하루였다"))
                .andExpect(jsonPath("$.data.diaryDate").value("2026-04-08"))
                .andExpect(jsonPath("$.data.primaryEmotion").value("기쁨"));
    }

    @Test
    @DisplayName("GET /api/v1/diaries - 일기 목록 조회 성공")
    void getDiaries_success() throws Exception {
        // given
        DiaryListResponse item1 = createDiaryListResponse(1L, "첫 번째 일기");
        DiaryListResponse item2 = createDiaryListResponse(2L, "두 번째 일기");
        Page<DiaryListResponse> page = new PageImpl<>(
                List.of(item1, item2),
                PageRequest.of(0, 20),
                2
        );
        given(diaryService.getDiaries(isNull(), any()))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/api/v1/diaries")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("첫 번째 일기"))
                .andExpect(jsonPath("$.data.content[1].id").value(2))
                .andExpect(jsonPath("$.data.content[1].title").value("두 번째 일기"))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("PATCH /api/v1/diaries/{id} - 일기 수정 성공")
    void updateDiary_success() throws Exception {
        // given
        DiaryResponse response = DiaryResponse.builder()
                .id(1L)
                .title("수정된 일기")
                .content("수정된 내용")
                .diaryDate(LocalDate.of(2026, 4, 8))
                .primaryEmotion("기쁨")
                .emotionScore(0.85)
                .summary("좋은 하루를 보냄")
                .riskScore(0)
                .isAnalyzed(true)
                .createdAt(LocalDateTime.of(2026, 4, 8, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 8, 10, 0))
                .build();

        given(diaryService.updateDiary(isNull(), eq(1L), any(UpdateDiaryRequest.class)))
                .willReturn(response);

        String requestJson = """
                {"title":"수정된 일기","content":"수정된 내용","diaryDate":"2026-04-08"}
                """;

        // when & then
        mockMvc.perform(patch("/api/v1/diaries/{diaryId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("일기가 수정되었습니다"))
                .andExpect(jsonPath("$.data.title").value("수정된 일기"))
                .andExpect(jsonPath("$.data.content").value("수정된 내용"));
    }

    @Test
    @DisplayName("DELETE /api/v1/diaries/{id} - 일기 삭제 성공")
    void deleteDiary_success() throws Exception {
        // given
        doNothing().when(diaryService).deleteDiary(isNull(), eq(1L));

        // when & then
        mockMvc.perform(delete("/api/v1/diaries/{diaryId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("일기가 삭제되었습니다"));
    }

    @Test
    @DisplayName("POST /api/v1/diaries/{id}/analyze - 일기 재분석 성공")
    void analyzeDiary_success() throws Exception {
        // given
        DiaryResponse response = createDiaryResponse(1L);
        given(diaryService.analyzeDiary(isNull(), eq(1L)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/diaries/{diaryId}/analyze", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("분석이 요청되었습니다"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.primaryEmotion").value("기쁨"))
                .andExpect(jsonPath("$.data.isAnalyzed").value(true));
    }

    // -- 테스트 데이터 생성 헬퍼 --

    private DiaryResponse createDiaryResponse(Long id) {
        return DiaryResponse.builder()
                .id(id)
                .title("오늘의 일기")
                .content("오늘은 좋은 하루였다")
                .diaryDate(LocalDate.of(2026, 4, 8))
                .primaryEmotion("기쁨")
                .emotionScore(0.85)
                .summary("좋은 하루를 보냄")
                .riskScore(0)
                .isAnalyzed(true)
                .createdAt(LocalDateTime.of(2026, 4, 8, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 8, 10, 0))
                .build();
    }

    private DiaryListResponse createDiaryListResponse(Long id, String title) {
        return DiaryListResponse.builder()
                .id(id)
                .title(title)
                .diaryDate(LocalDate.of(2026, 4, 8))
                .primaryEmotion("기쁨")
                .emotionScore(0.85)
                .isAnalyzed(true)
                .build();
    }
}
