// 파일: ChatReplyServiceTest.java
// 역할: 채팅 응답 서비스의 safety 우선 정책과 fallback 흐름을 검증한다.

package com.mindcompass.ai.service;

import com.mindcompass.ai.dto.request.GenerateReplyRequest;
import com.mindcompass.ai.dto.request.RiskScoreRequest;
import com.mindcompass.ai.dto.response.GenerateReplyResponse;
import com.mindcompass.ai.dto.response.RiskScoreResponse;
import com.mindcompass.ai.prompt.OpenAiPromptClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatReplyServiceTest {

    @InjectMocks
    private ChatReplyService chatReplyService;

    @Mock
    private RiskScoreService riskScoreService;

    @Mock
    private OpenAiPromptClient openAiPromptClient;

    @Test
    @DisplayName("위험 상황이면 안전 메시지를 반환하고 AI 응답은 생성하지 않는다")
    void generateReply_returnsSafetyMessage_whenRiskDetected() {
        // given
        GenerateReplyRequest request = createRequest("이제 정말 죽고 싶어요.");
        given(riskScoreService.analyze(org.mockito.ArgumentMatchers.any(RiskScoreRequest.class)))
                .willReturn(riskyResponse());

        // when
        GenerateReplyResponse response = chatReplyService.generateReply(request);

        // then
        assertThat(response.getResponseType()).isEqualTo("SAFETY");
        assertThat(response.getGenerated()).isTrue();
        assertThat(response.getDetectedEmotion()).isEqualTo("위기");
        verify(openAiPromptClient, never()).generateReply(request);
        verify(openAiPromptClient, never()).isAvailable();
    }

    @Test
    @DisplayName("AI가 비활성화되고 슬픔 표현이 있으면 슬픔 fallback을 반환한다")
    void generateReply_returnsSadnessFallback_whenAiUnavailableAndSadMessage() {
        // given
        GenerateReplyRequest request = createRequest("요즘 너무 힘들고 슬퍼요.");
        given(riskScoreService.analyze(org.mockito.ArgumentMatchers.any(RiskScoreRequest.class)))
                .willReturn(safeResponse());
        given(openAiPromptClient.isAvailable()).willReturn(false);

        // when
        GenerateReplyResponse response = chatReplyService.generateReply(request);

        // then
        assertThat(response.getResponseType()).isEqualTo("FALLBACK");
        assertThat(response.getGenerated()).isFalse();
        assertThat(response.getDetectedEmotion()).isEqualTo("슬픔");
        assertThat(response.getFailureReason()).isEqualTo("DEV_PROFILE");
    }

    @Test
    @DisplayName("AI가 비활성화되고 분노 표현이 있으면 분노 fallback을 반환한다")
    void generateReply_returnsAngerFallback_whenAiUnavailableAndAngryMessage() {
        // given
        GenerateReplyRequest request = createRequest("계속 짜증 나고 화가 나요.");
        given(riskScoreService.analyze(org.mockito.ArgumentMatchers.any(RiskScoreRequest.class)))
                .willReturn(safeResponse());
        given(openAiPromptClient.isAvailable()).willReturn(false);

        // when
        GenerateReplyResponse response = chatReplyService.generateReply(request);

        // then
        assertThat(response.getResponseType()).isEqualTo("FALLBACK");
        assertThat(response.getGenerated()).isFalse();
        assertThat(response.getDetectedEmotion()).isEqualTo("분노");
        assertThat(response.getFailureReason()).isEqualTo("DEV_PROFILE");
    }

    @Test
    @DisplayName("AI가 활성화되고 응답 생성에 성공하면 AI 응답을 반환한다")
    void generateReply_returnsAiResponse_whenAiAvailableAndSuccess() {
        // given
        GenerateReplyRequest request = createRequest("오늘 하루가 막막했어요.");
        GenerateReplyResponse aiResponse = GenerateReplyResponse.success("많이 버거운 하루였겠어요.", "불안");
        given(riskScoreService.analyze(org.mockito.ArgumentMatchers.any(RiskScoreRequest.class)))
                .willReturn(safeResponse());
        given(openAiPromptClient.isAvailable()).willReturn(true);
        given(openAiPromptClient.generateReply(request)).willReturn(Optional.of(aiResponse));

        // when
        GenerateReplyResponse response = chatReplyService.generateReply(request);

        // then
        assertThat(response).isSameAs(aiResponse);
        assertThat(response.getGenerated()).isTrue();
        assertThat(response.getResponseType()).isEqualTo("AI");

        ArgumentCaptor<RiskScoreRequest> captor = ArgumentCaptor.forClass(RiskScoreRequest.class);
        verify(riskScoreService).analyze(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo(request.getUserMessage());
        assertThat(captor.getValue().getUserId()).isEqualTo(request.getUserId());
        assertThat(captor.getValue().getContextType()).isEqualTo("CHAT");
    }

    @Test
    @DisplayName("AI가 활성화되었지만 응답 생성에 실패하면 fallback을 반환한다")
    void generateReply_returnsFallback_whenAiAvailableButEmpty() {
        // given
        GenerateReplyRequest request = createRequest("무슨 말을 해야 할지 모르겠어요.");
        given(riskScoreService.analyze(org.mockito.ArgumentMatchers.any(RiskScoreRequest.class)))
                .willReturn(safeResponse());
        given(openAiPromptClient.isAvailable()).willReturn(true);
        given(openAiPromptClient.generateReply(request)).willReturn(Optional.empty());

        // when
        GenerateReplyResponse response = chatReplyService.generateReply(request);

        // then
        assertThat(response.getResponseType()).isEqualTo("FALLBACK");
        assertThat(response.getGenerated()).isFalse();
        assertThat(response.getFailureReason()).isEqualTo("AI 응답 생성 실패");
    }

    private GenerateReplyRequest createRequest(String userMessage) {
        return GenerateReplyRequest.builder()
                .sessionId(10L)
                .userId(1L)
                .userMessage(userMessage)
                .safetyMode(false)
                .conversationHistory(List.of(
                        GenerateReplyRequest.ChatMessage.builder()
                                .role("USER")
                                .content("이전에도 비슷하게 힘들었어요.")
                                .build(),
                        GenerateReplyRequest.ChatMessage.builder()
                                .role("ASSISTANT")
                                .content("어떤 순간이 가장 버거웠는지 말씀해 주세요.")
                                .build()
                ))
                .build();
    }

    private RiskScoreResponse safeResponse() {
        return RiskScoreResponse.safe();
    }

    private RiskScoreResponse riskyResponse() {
        return RiskScoreResponse.builder()
                .isRisky(true)
                .riskScore(95)
                .riskType("SUICIDE")
                .detectedKeywords(List.of("죽고 싶"))
                .analysisMethod("KEYWORD")
                .recommendedAction("SHOW_SAFETY_MESSAGE")
                .build();
    }
}
