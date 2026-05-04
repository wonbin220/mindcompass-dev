// 파일: UpdateDiaryRequest.java
// 역할: 일기 수정 요청 DTO
// 호출: DiaryController -> DiaryService

package com.mindcompass.api.diary.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class UpdateDiaryRequest {

    @Size(max = 200, message = "제목은 200자 이내여야 합니다")
    private String title;

    @Size(max = 10000, message = "내용은 10000자 이내여야 합니다")
    private String content;

    private LocalDateTime writtenAt;
}
