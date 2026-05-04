// 파일: DiaryAnalysisResponse.java
// 역할: 일기 분석 응답 DTO
// 호출: AiDiaryAnalysisClient -> DiaryService

package com.mindcompass.api.infra.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
public class DiaryAnalysisResponse {

    private String primaryEmotion;       // 주요 감정 (예: "기쁨", "슬픔")
    private Integer emotionIntensity;    // 감정 강도
    private List<String> keywords;       // 핵심 키워드
    private String summary;              // 한 줄 요약
    private BigDecimal confidence;       // 분석 신뢰도
    private String rawPayload;           // 원본 분석 payload
    private String riskLevel;            // 위험도 레벨
    private BigDecimal riskScore;        // 위험도 점수
    private String riskSignals;          // 위험 신호
    private String recommendedAction;    // 추천 액션
    private Boolean requiresSafetyCheck; // 안전 확인 필요 여부
}
