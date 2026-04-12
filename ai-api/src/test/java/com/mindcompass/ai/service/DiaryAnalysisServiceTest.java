// 파일: DiaryAnalysisServiceTest.java
// 역할: 일기 분석 서비스의 AI 성공/실패와 개발용 fallback 흐름을 검증한다.

package com.mindcompass.ai.service;

import com.mindcompass.ai.dto.request.AnalyzeDiaryRequest;
import com.mindcompass.ai.dto.response.AnalyzeDiaryResponse;
import com.mindcompass.ai.prompt.OpenAiPromptClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DiaryAnalysisServiceTest {

    @InjectMocks
    private DiaryAnalysisService diaryAnalysisService;

    @Mock
    private OpenAiPromptClient openAiPromptClient;

    @Test
    @DisplayName("AI가 비활성화되고 행복 키워드가 있으면 기쁨 fallback을 반환한다")
    void analyze_returnsJoyFallback_whenAiUnavailableAndHappyContent() {
        // given
        AnalyzeDiaryRequest request = createRequest("오늘 정말 행복하고 기쁜 하루였어. 모든 게 좋았어.");
        given(openAiPromptClient.isAvailable()).willReturn(false);

        // when
        AnalyzeDiaryResponse response = diaryAnalysisService.analyze(request);

        // then
        assertThat(response.getPrimaryEmotion()).isEqualTo("기쁨");
        assertThat(response.getEmotionScore()).isEqualTo(0.8);
        assertThat(response.getAnalyzed()).isFalse();
        assertThat(response.getFailureReason()).isEqualTo("DEV_PROFILE");
    }

    @Test
    @DisplayName("AI가 비활성화되고 슬픔 키워드가 있으면 슬픔 fallback을 반환한다")
    void analyze_returnsSadnessFallback_whenAiUnavailableAndSadContent() {
        // given
        AnalyzeDiaryRequest request = createRequest("하루 종일 우울하고 너무 힘들어서 슬펐어.");
        given(openAiPromptClient.isAvailable()).willReturn(false);

        // when
        AnalyzeDiaryResponse response = diaryAnalysisService.analyze(request);

        // then
        assertThat(response.getPrimaryEmotion()).isEqualTo("슬픔");
        assertThat(response.getEmotionScore()).isEqualTo(0.3);
        assertThat(response.getAnalyzed()).isFalse();
        assertThat(response.getFailureReason()).isEqualTo("DEV_PROFILE");
    }

    @Test
    @DisplayName("AI가 비활성화되고 감정 키워드가 없으면 평온 fallback을 반환한다")
    void analyze_returnsCalmFallback_whenAiUnavailableAndNeutralContent() {
        // given
        AnalyzeDiaryRequest request = createRequest("오늘은 평범하게 일하고 집에 와서 쉬었다.");
        given(openAiPromptClient.isAvailable()).willReturn(false);

        // when
        AnalyzeDiaryResponse response = diaryAnalysisService.analyze(request);

        // then
        assertThat(response.getPrimaryEmotion()).isEqualTo("평온");
        assertThat(response.getEmotionScore()).isEqualTo(0.5);
        assertThat(response.getAnalyzed()).isFalse();
        assertThat(response.getFailureReason()).isEqualTo("DEV_PROFILE");
    }

    @Test
    @DisplayName("AI가 활성화되고 분석에 성공하면 AI 결과를 그대로 반환한다")
    void analyze_returnsAiResponse_whenAiAvailableAndSuccess() {
        // given
        AnalyzeDiaryRequest request = createRequest("오늘은 걱정이 많았지만 잘 버텼다.");
        AnalyzeDiaryResponse aiResponse = AnalyzeDiaryResponse.success("불안", 0.35, "걱정이 있었지만 버틴 하루다.");
        given(openAiPromptClient.isAvailable()).willReturn(true);
        given(openAiPromptClient.analyzeDiary(request)).willReturn(Optional.of(aiResponse));

        // when
        AnalyzeDiaryResponse response = diaryAnalysisService.analyze(request);

        // then
        assertThat(response).isSameAs(aiResponse);
        verify(openAiPromptClient).analyzeDiary(request);
    }

    @Test
    @DisplayName("AI가 활성화되었지만 결과가 없으면 분석 실패 fallback을 반환한다")
    void analyze_returnsFallback_whenAiAvailableButEmpty() {
        // given
        AnalyzeDiaryRequest request = createRequest("오늘은 무기력했다.");
        given(openAiPromptClient.isAvailable()).willReturn(true);
        given(openAiPromptClient.analyzeDiary(request)).willReturn(Optional.empty());

        // when
        AnalyzeDiaryResponse response = diaryAnalysisService.analyze(request);

        // then
        assertThat(response.getPrimaryEmotion()).isEqualTo("미분석");
        assertThat(response.getEmotionScore()).isEqualTo(0.5);
        assertThat(response.getSummary()).isEqualTo("AI 분석을 수행할 수 없습니다.");
        assertThat(response.getAnalyzed()).isFalse();
        assertThat(response.getFailureReason()).isEqualTo("AI 분석 실패");
    }

    private AnalyzeDiaryRequest createRequest(String content) {
        return AnalyzeDiaryRequest.builder()
                .diaryId(101L)
                .userId(1L)
                .title("테스트 일기")
                .content(content)
                .build();
    }
}
