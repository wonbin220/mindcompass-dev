// 파일: RiskScoreResponse.java
// 역할: 위험도 점수 응답 DTO
// 설명: backend-api가 기대하는 위험도 평가 결과 구조
// 주의: 이 계약을 변경하면 backend-api도 수정해야 함

package com.mindcompass.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskScoreResponse {

    /**
     * 위험 여부
     * - true이면 즉각 안전 대응 필요
     */
    private Boolean isRisky;

    /**
     * 위험도 점수
     * - 0 ~ 100 사이 정수
     * - 70 이상이면 고위험
     */
    private Integer riskScore;

    /**
     * 위험 유형 (고위험 시에만)
     * - "SELF_HARM": 자해 위험
     * - "SUICIDE": 자살 위험
     * - "CRISIS": 기타 위기 상황
     * - null: 위험 없음
     */
    private String riskType;

    /**
     * 감지된 위험 키워드 목록
     * - 로깅 및 분석용
     */
    private List<String> detectedKeywords;

    /**
     * 분석 방법
     * - "AI": AI 기반 분석
     * - "KEYWORD": 키워드 기반 분석 (fallback)
     */
    private String analysisMethod;

    /**
     * 권장 대응
     * - 안전 메시지 표시 여부 등
     */
    private String recommendedAction;

    /**
     * 안전 상태 Fallback 응답 (키워드 분석 기반)
     */
    public static RiskScoreResponse safe() {
        return RiskScoreResponse.builder()
                .isRisky(false)
                .riskScore(0)
                .riskType(null)
                .detectedKeywords(List.of())
                .analysisMethod("KEYWORD")
                .recommendedAction("NONE")
                .build();
    }

    /**
     * 위험 감지 응답 (키워드 분석 기반)
     */
    public static RiskScoreResponse risky(String riskType, List<String> keywords) {
        return RiskScoreResponse.builder()
                .isRisky(true)
                .riskScore(80)
                .riskType(riskType)
                .detectedKeywords(keywords)
                .analysisMethod("KEYWORD")
                .recommendedAction("SHOW_SAFETY_MESSAGE")
                .build();
    }
}
