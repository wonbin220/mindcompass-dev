// 파일: AnalyzeDiaryRequest.java
// 역할: 일기 분석 요청 DTO
// 설명: backend-api가 보내는 일기 분석 요청 구조

package com.mindcompass.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeDiaryRequest {

    /**
     * 일기 ID
     * - 로깅 및 추적용
     */
    @NotNull(message = "diaryId는 필수입니다")
    private Long diaryId;

    /**
     * 일기 제목
     * - 감정 분석 컨텍스트에 활용
     */
    private String title;

    /**
     * 일기 본문
     * - 주요 분석 대상
     */
    @NotBlank(message = "content는 필수입니다")
    private String content;

    /**
     * 사용자 ID
     * - 개인화된 분석에 활용 (추후)
     */
    private Long userId;
}
