// 파일: DiaryEmotionTagResponse.java
// 역할: 일기 감정 태그 응답 DTO
// 호출: DiaryResponse -> DiaryController

package com.mindcompass.api.diary.dto.response;

import com.mindcompass.api.diary.domain.DiaryEmotionTag;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DiaryEmotionTagResponse {

    private final Long id;
    private final String emotionCode;
    private final Integer intensity;
    private final String sourceType;

    public static DiaryEmotionTagResponse from(DiaryEmotionTag tag) {
        return DiaryEmotionTagResponse.builder()
                .id(tag.getId())
                .emotionCode(tag.getEmotionCode())
                .intensity(tag.getIntensity())
                .sourceType(tag.getSourceType())
                .build();
    }
}
