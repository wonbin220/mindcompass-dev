// 파일: CreateDiaryRequest.java
// 역할: 일기 생성 요청 DTO
// 화면: 일기 작성 페이지

package com.mindcompass.api.diary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class CreateDiaryRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자 이내여야 합니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 10000, message = "내용은 10000자 이내여야 합니다")
    private String content;

    @NotNull(message = "일기 날짜는 필수입니다")
    private LocalDate diaryDate;
}
