// 파일: GenerateReplyResponse.java
// 역할: 채팅 응답 생성 결과 DTO
// 설명: backend-api가 기대하는 AI 응답 구조
// 주의: 이 계약을 변경하면 backend-api도 수정해야 함

package com.mindcompass.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateReplyResponse {

    /**
     * AI 응답 메시지
     * - 사용자에게 표시할 내용
     */
    private String reply;

    /**
     * 감지된 감정 (선택적)
     * - 사용자 메시지에서 감지된 감정
     */
    private String detectedEmotion;

    /**
     * 응답 생성 성공 여부
     * - false이면 fallback 응답
     */
    private Boolean generated;

    /**
     * 응답 유형
     * - "AI": AI 생성 응답
     * - "FALLBACK": 기본 응답
     * - "SAFETY": 안전 대응 메시지
     */
    private String responseType;

    /**
     * 실패 사유 (실패 시에만)
     */
    private String failureReason;

    /**
     * 기본 Fallback 응답
     */
    public static GenerateReplyResponse fallback(String reason) {
        return GenerateReplyResponse.builder()
                .reply("지금은 응답을 생성하기 어렵습니다. 잠시 후 다시 시도해 주세요.")
                .detectedEmotion(null)
                .generated(false)
                .responseType("FALLBACK")
                .failureReason(reason)
                .build();
    }

    /**
     * 안전 대응 메시지
     */
    public static GenerateReplyResponse safetyMessage() {
        return GenerateReplyResponse.builder()
                .reply("지금 많이 힘드시군요. 혼자 감당하기 어려운 상황이라면, " +
                       "전문 상담을 받아보시는 것이 도움이 될 수 있어요. " +
                       "자살예방상담전화 1393, 정신건강위기상담전화 1577-0199로 연락해 보세요.")
                .detectedEmotion("위기")
                .generated(true)
                .responseType("SAFETY")
                .build();
    }

    /**
     * 성공 응답
     */
    public static GenerateReplyResponse success(String reply, String emotion) {
        return GenerateReplyResponse.builder()
                .reply(reply)
                .detectedEmotion(emotion)
                .generated(true)
                .responseType("AI")
                .build();
    }
}
