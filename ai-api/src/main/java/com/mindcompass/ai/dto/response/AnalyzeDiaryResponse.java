// 파일: AnalyzeDiaryResponse.java
// 역할: 일기 분석 응답 DTO
// 설명: backend-api가 기대하는 일기 분석 결과 구조
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
public class AnalyzeDiaryResponse {

    /**
     * 주요 감정
     * - 분석된 일기의 대표 감정
     * - 예: "기쁨", "슬픔", "불안", "분노", "평온"
     */
    private String primaryEmotion;

    /**
     * 감정 점수
     * - 0.0 ~ 1.0 사이의 긍정/부정 점수
     * - 1.0에 가까울수록 긍정적
     */
    private Double emotionScore;

    /**
     * 일기 요약
     * - AI가 생성한 2~3문장 요약
     */
    private String summary;

    /**
     * 분석 성공 여부
     * - false인 경우 fallback 응답임을 의미
     */
    private Boolean analyzed;

    /**
     * 분석 실패 사유 (실패 시에만)
     */
    private String failureReason;

    /**
     * Fallback 응답 생성
     */
    public static AnalyzeDiaryResponse fallback(String reason) {
        return AnalyzeDiaryResponse.builder()
                .primaryEmotion("미분석")
                .emotionScore(0.5)
                .summary("AI 분석을 수행할 수 없습니다.")
                .analyzed(false)
                .failureReason(reason)
                .build();
    }

    /**
     * 성공 응답 생성
     */
    public static AnalyzeDiaryResponse success(String emotion, Double score, String summary) {
        return AnalyzeDiaryResponse.builder()
                .primaryEmotion(emotion)
                .emotionScore(score)
                .summary(summary)
                .analyzed(true)
                .build();
    }
}
