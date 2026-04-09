// 파일: SafetyCheckResponse.java
// 역할: 안전 확인 응답 DTO
// 설명: 위기 신호 감지 결과

package com.mindcompass.api.infra.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafetyCheckResponse {

    private Boolean isRisky;             // 위험 여부
    private Integer riskScore;           // 위험도 점수 (0 ~ 100)
    private String riskLevel;            // 위험 수준 (LOW, MEDIUM, HIGH, CRITICAL)
    private String recommendedAction;    // 권장 조치
    private String safetyMessage;        // 안전 메시지 (위기 시 보여줄 고정 메시지)
}
