// 파일: InternalAiControllerTest.java
// 역할: 내부 AI 컨트롤러의 성공 및 fallback 응답 계약을 검증한다.

package com.mindcompass.ai.controller;

import com.mindcompass.ai.dto.response.AnalyzeDiaryResponse;
import com.mindcompass.ai.dto.response.GenerateReplyResponse;
import com.mindcompass.ai.dto.response.RiskScoreResponse;
import com.mindcompass.ai.service.ChatReplyService;
import com.mindcompass.ai.service.DiaryAnalysisService;
import com.mindcompass.ai.service.RiskScoreService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalAiController.class)
@AutoConfigureMockMvc(addFilters = false)
class InternalAiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiaryAnalysisService diaryAnalysisService;

    @MockitoBean
    private RiskScoreService riskScoreService;

    @MockitoBean
    private ChatReplyService chatReplyService;

    @Test
    @DisplayName("POST /internal/ai/analyze-diary - 일기 분석 성공 응답을 반환한다")
    void analyzeDiary_success() throws Exception {
        // given
        AnalyzeDiaryResponse response = AnalyzeDiaryResponse.success(
                "기쁨",
                0.87,
                "오늘은 안도감과 성취감을 느낀 하루였다."
        );
        given(diaryAnalysisService.analyze(any())).willReturn(response);

        String requestJson = """
                {
                  "diaryId": 101,
                  "title": "좋은 하루",
                  "content": "오늘은 프로젝트를 마무리해서 정말 뿌듯했다.",
                  "userId": 1
                }
                """;

        // when & then
        mockMvc.perform(post("/internal/ai/analyze-diary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryEmotion").value("기쁨"))
                .andExpect(jsonPath("$.emotionScore").value(0.87))
                .andExpect(jsonPath("$.summary").value("오늘은 안도감과 성취감을 느낀 하루였다."))
                .andExpect(jsonPath("$.analyzed").value(true))
                .andExpect(jsonPath("$.failureReason").doesNotExist());
    }

    @Test
    @DisplayName("POST /internal/ai/analyze-diary - 분석 실패 fallback도 200으로 반환한다")
    void analyzeDiary_fallbackStillReturnsOk() throws Exception {
        // given
        AnalyzeDiaryResponse response = AnalyzeDiaryResponse.fallback("AI 분석 실패");
        given(diaryAnalysisService.analyze(any())).willReturn(response);

        String requestJson = """
                {
                  "diaryId": 102,
                  "title": "무거운 하루",
                  "content": "오늘은 아무것도 손에 잡히지 않았다.",
                  "userId": 1
                }
                """;

        // when & then
        mockMvc.perform(post("/internal/ai/analyze-diary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryEmotion").value("미분석"))
                .andExpect(jsonPath("$.emotionScore").value(0.5))
                .andExpect(jsonPath("$.summary").value("AI 분석을 수행할 수 없습니다."))
                .andExpect(jsonPath("$.analyzed").value(false))
                .andExpect(jsonPath("$.failureReason").value("AI 분석 실패"));
    }

    @Test
    @DisplayName("POST /internal/ai/risk-score - 위험도 분석 성공 응답을 반환한다")
    void riskScore_success() throws Exception {
        // given
        RiskScoreResponse response = RiskScoreResponse.builder()
                .isRisky(true)
                .riskScore(78)
                .riskType("SUICIDE")
                .detectedKeywords(List.of("죽고 싶", "사라지고 싶"))
                .analysisMethod("HYBRID")
                .recommendedAction("SHOW_SAFETY_MESSAGE")
                .build();
        given(riskScoreService.analyze(any())).willReturn(response);

        String requestJson = """
                {
                  "content": "요즘은 그냥 사라지고 싶다는 생각이 들어요.",
                  "userId": 1,
                  "contextType": "CHAT"
                }
                """;

        // when & then
        mockMvc.perform(post("/internal/ai/risk-score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRisky").value(true))
                .andExpect(jsonPath("$.riskScore").value(78))
                .andExpect(jsonPath("$.riskType").value("SUICIDE"))
                .andExpect(jsonPath("$.detectedKeywords[0]").value("죽고 싶"))
                .andExpect(jsonPath("$.detectedKeywords[1]").value("사라지고 싶"))
                .andExpect(jsonPath("$.analysisMethod").value("HYBRID"))
                .andExpect(jsonPath("$.recommendedAction").value("SHOW_SAFETY_MESSAGE"));
    }

    @Test
    @DisplayName("POST /internal/ai/risk-score - fallback 위험도 응답도 200으로 반환한다")
    void riskScore_fallbackStillReturnsOk() throws Exception {
        // given
        RiskScoreResponse response = RiskScoreResponse.safe();
        given(riskScoreService.analyze(any())).willReturn(response);

        String requestJson = """
                {
                  "content": "오늘은 그냥 쉬고 싶어요.",
                  "userId": 1,
                  "contextType": "DIARY"
                }
                """;

        // when & then
        mockMvc.perform(post("/internal/ai/risk-score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRisky").value(false))
                .andExpect(jsonPath("$.riskScore").value(0))
                .andExpect(jsonPath("$.riskType").doesNotExist())
                .andExpect(jsonPath("$.detectedKeywords").isArray())
                .andExpect(jsonPath("$.detectedKeywords").isEmpty())
                .andExpect(jsonPath("$.analysisMethod").value("KEYWORD"))
                .andExpect(jsonPath("$.recommendedAction").value("NONE"));
    }

    @Test
    @DisplayName("POST /internal/ai/generate-reply - 채팅 응답 생성 성공 결과를 반환한다")
    void generateReply_success() throws Exception {
        // given
        GenerateReplyResponse response = GenerateReplyResponse.success(
                "많이 지친 하루였겠어요. 지금 가장 버거운 순간부터 천천히 이야기해 주세요.",
                "불안"
        );
        given(chatReplyService.generateReply(any())).willReturn(response);

        String requestJson = """
                {
                  "sessionId": 10,
                  "userId": 1,
                  "userMessage": "오늘 하루가 너무 버거웠어요.",
                  "conversationHistory": [
                    {
                      "role": "USER",
                      "content": "어제도 비슷하게 힘들었어요."
                    },
                    {
                      "role": "ASSISTANT",
                      "content": "특히 어떤 순간이 가장 힘들었나요?"
                    }
                  ],
                  "safetyMode": false
                }
                """;

        // when & then
        mockMvc.perform(post("/internal/ai/generate-reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("많이 지친 하루였겠어요. 지금 가장 버거운 순간부터 천천히 이야기해 주세요."))
                .andExpect(jsonPath("$.detectedEmotion").value("불안"))
                .andExpect(jsonPath("$.generated").value(true))
                .andExpect(jsonPath("$.responseType").value("AI"))
                .andExpect(jsonPath("$.failureReason").doesNotExist());
    }

    @Test
    @DisplayName("POST /internal/ai/generate-reply - fallback 응답도 200으로 반환한다")
    void generateReply_fallbackStillReturnsOk() throws Exception {
        // given
        GenerateReplyResponse response = GenerateReplyResponse.fallback("AI 응답 생성 실패");
        given(chatReplyService.generateReply(any())).willReturn(response);

        String requestJson = """
                {
                  "sessionId": 11,
                  "userId": 1,
                  "userMessage": "무슨 말을 해야 할지 모르겠어요.",
                  "conversationHistory": [],
                  "safetyMode": false
                }
                """;

        // when & then
        mockMvc.perform(post("/internal/ai/generate-reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("지금은 응답을 생성하기 어렵습니다. 잠시 후 다시 시도해 주세요."))
                .andExpect(jsonPath("$.detectedEmotion").doesNotExist())
                .andExpect(jsonPath("$.generated").value(false))
                .andExpect(jsonPath("$.responseType").value("FALLBACK"))
                .andExpect(jsonPath("$.failureReason").value("AI 응답 생성 실패"));
    }
}
