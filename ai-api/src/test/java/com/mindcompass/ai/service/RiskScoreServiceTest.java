// 파일: RiskScoreServiceTest.java
// 역할: 위험도 분석 서비스의 키워드 우선 정책과 AI 병합 로직을 검증한다.

package com.mindcompass.ai.service;

import com.mindcompass.ai.dto.request.RiskScoreRequest;
import com.mindcompass.ai.dto.response.RiskScoreResponse;
import com.mindcompass.ai.prompt.KeywordRiskAnalyzer;
import com.mindcompass.ai.prompt.OpenAiPromptClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RiskScoreServiceTest {

    @InjectMocks
    private RiskScoreService riskScoreService;

    @Mock
    private KeywordRiskAnalyzer keywordRiskAnalyzer;

    @Mock
    private OpenAiPromptClient openAiPromptClient;

    @Test
    @DisplayName("고위험 키워드가 감지되면 AI 호출 없이 즉시 반환한다")
    void analyze_returnsImmediately_whenHighRiskKeywordDetected() {
        // given
        RiskScoreRequest request = createRequest("정말 죽고 싶어요.");
        RiskScoreResponse keywordResult = RiskScoreResponse.builder()
                .isRisky(true)
                .riskScore(90)
                .riskType("SUICIDE")
                .detectedKeywords(List.of("죽고 싶"))
                .analysisMethod("KEYWORD")
                .recommendedAction("SHOW_SAFETY_MESSAGE")
                .build();
        given(keywordRiskAnalyzer.analyze(request.getContent())).willReturn(keywordResult);

        // when
        RiskScoreResponse response = riskScoreService.analyze(request);

        // then
        assertThat(response).isSameAs(keywordResult);
        verify(openAiPromptClient, never()).analyzeRisk(any(RiskScoreRequest.class));
        verify(openAiPromptClient, never()).isAvailable();
    }

    @Test
    @DisplayName("AI가 비활성화되면 키워드 분석 결과만 반환한다")
    void analyze_returnsKeywordResult_whenAiUnavailable() {
        // given
        RiskScoreRequest request = createRequest("버티기 힘들어요.");
        RiskScoreResponse keywordResult = RiskScoreResponse.builder()
                .isRisky(false)
                .riskScore(60)
                .riskType(null)
                .detectedKeywords(List.of("버틸 수 없"))
                .analysisMethod("KEYWORD")
                .recommendedAction("MONITOR")
                .build();
        given(keywordRiskAnalyzer.analyze(request.getContent())).willReturn(keywordResult);
        given(openAiPromptClient.isAvailable()).willReturn(false);

        // when
        RiskScoreResponse response = riskScoreService.analyze(request);

        // then
        assertThat(response).isSameAs(keywordResult);
        verify(openAiPromptClient, never()).analyzeRisk(any(RiskScoreRequest.class));
    }

    @Test
    @DisplayName("AI 분석에 성공하면 더 높은 점수를 기준으로 HYBRID 결과를 반환한다")
    void analyze_returnsHybridResult_whenAiAvailableAndSuccess() {
        // given
        RiskScoreRequest request = createRequest("요즘 너무 불안해요.");
        RiskScoreResponse keywordResult = RiskScoreResponse.builder()
                .isRisky(false)
                .riskScore(40)
                .riskType(null)
                .detectedKeywords(List.of("불안"))
                .analysisMethod("KEYWORD")
                .recommendedAction("MONITOR")
                .build();
        RiskScoreResponse aiResult = RiskScoreResponse.builder()
                .isRisky(true)
                .riskScore(75)
                .riskType("SUICIDE")
                .detectedKeywords(List.of())
                .analysisMethod("AI")
                .recommendedAction("SHOW_SAFETY_MESSAGE")
                .build();
        given(keywordRiskAnalyzer.analyze(request.getContent())).willReturn(keywordResult);
        given(openAiPromptClient.isAvailable()).willReturn(true);
        given(openAiPromptClient.analyzeRisk(request)).willReturn(Optional.of(aiResult));

        // when
        RiskScoreResponse response = riskScoreService.analyze(request);

        // then
        assertThat(response.getIsRisky()).isTrue();
        assertThat(response.getRiskScore()).isEqualTo(75);
        assertThat(response.getRiskType()).isEqualTo("SUICIDE");
        assertThat(response.getDetectedKeywords()).containsExactly("불안");
        assertThat(response.getAnalysisMethod()).isEqualTo("HYBRID");
        assertThat(response.getRecommendedAction()).isEqualTo("SHOW_SAFETY_MESSAGE");
    }

    @Test
    @DisplayName("AI 분석 결과가 비어 있으면 키워드 결과를 그대로 반환한다")
    void analyze_returnsKeywordResult_whenAiAvailableButEmpty() {
        // given
        RiskScoreRequest request = createRequest("아무 의미가 없다고 느껴져요.");
        RiskScoreResponse keywordResult = RiskScoreResponse.builder()
                .isRisky(false)
                .riskScore(60)
                .riskType(null)
                .detectedKeywords(List.of("아무 의미"))
                .analysisMethod("KEYWORD")
                .recommendedAction("MONITOR")
                .build();
        given(keywordRiskAnalyzer.analyze(request.getContent())).willReturn(keywordResult);
        given(openAiPromptClient.isAvailable()).willReturn(true);
        given(openAiPromptClient.analyzeRisk(request)).willReturn(Optional.empty());

        // when
        RiskScoreResponse response = riskScoreService.analyze(request);

        // then
        assertThat(response).isSameAs(keywordResult);
        verify(openAiPromptClient).analyzeRisk(request);
    }

    private RiskScoreRequest createRequest(String content) {
        return RiskScoreRequest.builder()
                .content(content)
                .userId(1L)
                .contextType("CHAT")
                .build();
    }
}
