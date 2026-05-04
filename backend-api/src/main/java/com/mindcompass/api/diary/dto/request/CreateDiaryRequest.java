// 파일: CreateDiaryRequest.java
// 역할: 일기 생성 요청 DTO
// 호출: DiaryController -> DiaryService

package com.mindcompass.api.diary.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class CreateDiaryRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자 이내여야 합니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 10000, message = "내용은 10000자 이내여야 합니다")
    private String content;

    @NotNull(message = "작성 시각은 필수입니다")
    private LocalDateTime writtenAt;

    private String primaryEmotion;

    @Min(value = 1, message = "감정 강도는 1 이상이어야 합니다")
    @Max(value = 5, message = "감정 강도는 5 이하여야 합니다")
    private Integer emotionIntensity;
}
