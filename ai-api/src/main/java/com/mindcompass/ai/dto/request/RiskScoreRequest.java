// 파일: RiskScoreRequest.java
// 역할: 위험도 점수 요청 DTO
// 설명: 채팅/일기 내용의 위험도 평가 요청 구조

package com.mindcompass.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskScoreRequest {

    /**
     * 분석할 텍스트
     * - 사용자 메시지 또는 일기 내용
     */
    @NotBlank(message = "content는 필수입니다")
    private String content;

    /**
     * 사용자 ID
     * - 로깅 및 추적용
     */
    private Long userId;

    /**
     * 컨텍스트 타입
     * - "CHAT" 또는 "DIARY"
     * - 분석 기준이 달라질 수 있음
     */
    private String contextType;
}
