// 파일: ChatResponse.java
// 역할: 채팅 AI 응답 DTO
// 설명: ai-api로부터 받은 채팅 응답 결과

package com.mindcompass.api.infra.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private String message;              // AI 응답 메시지
    private Boolean isSafetyTriggered;   // 안전 분기 트리거 여부
    private String safetyType;           // 안전 유형 (null이면 일반 응답)
    private Integer emotionScore;        // 감정 점수
    private String detectedEmotion;      // 감지된 감정
}
