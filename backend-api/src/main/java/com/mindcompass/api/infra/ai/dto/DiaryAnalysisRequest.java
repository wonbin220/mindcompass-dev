// 파일: DiaryAnalysisRequest.java
// 역할: 일기 분석 요청 DTO
// 설명: ai-api로 일기 분석을 요청할 때 사용하는 DTO

package com.mindcompass.api.infra.ai.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DiaryAnalysisRequest {

    private final Long diaryId;
    private final Long userId;
    private final String content;
    private final String title;
}
