// 파일: DiaryAnalysisResponse.java
// 역할: 일기 분석 응답 DTO
// 설명: ai-api로부터 받은 일기 분석 결과

package com.mindcompass.api.infra.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class DiaryAnalysisResponse {

    private String primaryEmotion;       // 주요 감정 (예: "기쁨", "슬픔")
    private Double emotionScore;         // 감정 강도 (0.0 ~ 1.0)
    private List<String> keywords;       // 핵심 키워드
    private String summary;              // 한 줄 요약
    private Integer riskScore;           // 위험도 점수 (0 ~ 100)
    private Boolean requiresSafetyCheck; // 안전 확인 필요 여부
}
