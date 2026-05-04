// 파일: DiaryAiAnalysisResponse.java
// 역할: 일기 AI 분석 응답 DTO
// 호출: DiaryResponse -> DiaryController

package com.mindcompass.api.diary.dto.response;

import com.mindcompass.api.diary.domain.DiaryAiAnalysis;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class DiaryAiAnalysisResponse {

    private final Long id;
    private final String primaryEmotion;
    private final Integer emotionIntensity;
    private final String summary;
    private final BigDecimal confidence;
    private final String riskLevel;
    private final BigDecimal riskScore;
    private final String recommendedAction;
    private final LocalDateTime createdAt;

    public static DiaryAiAnalysisResponse from(DiaryAiAnalysis analysis) {
        return DiaryAiAnalysisResponse.builder()
                .id(analysis.getId())
                .primaryEmotion(analysis.getPrimaryEmotion())
                .emotionIntensity(analysis.getEmotionIntensity())
                .summary(analysis.getSummary())
                .confidence(analysis.getConfidence())
                .riskLevel(analysis.getRiskLevel())
                .riskScore(analysis.getRiskScore())
                .recommendedAction(analysis.getRecommendedAction())
                .createdAt(analysis.getCreatedAt())
                .build();
    }
}
