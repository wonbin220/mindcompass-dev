// 파일: GenerateReplyRequest.java
// 역할: 채팅 응답 생성 요청 DTO
// 설명: 사용자 메시지에 대한 AI 응답 생성 요청 구조

package com.mindcompass.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateReplyRequest {

    /**
     * 세션 ID
     * - 대화 컨텍스트 추적용
     */
    @NotNull(message = "sessionId는 필수입니다")
    private Long sessionId;

    /**
     * 사용자 ID
     * - 개인화 및 로깅용
     */
    @NotNull(message = "userId는 필수입니다")
    private Long userId;

    /**
     * 현재 사용자 메시지
     * - AI가 응답해야 할 메시지
     */
    @NotBlank(message = "userMessage는 필수입니다")
    private String userMessage;

    /**
     * 이전 대화 히스토리
     * - 컨텍스트 유지를 위한 최근 메시지들
     */
    private List<ChatMessage> conversationHistory;

    /**
     * 안전 모드 활성화 여부
     * - true이면 더 조심스러운 응답 생성
     */
    private Boolean safetyMode;

    /**
     * 대화 히스토리 내 메시지 구조
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        /**
         * 발신자: "USER" 또는 "ASSISTANT"
         */
        private String role;

        /**
         * 메시지 내용
         */
        private String content;
    }
}
