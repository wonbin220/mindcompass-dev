// 파일: SendMessageRequest.java
// 역할: 메시지 전송 요청 DTO
// 화면: 채팅 화면

package com.mindcompass.api.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SendMessageRequest {

    @NotBlank(message = "메시지는 필수입니다")
    @Size(max = 2000, message = "메시지는 2000자 이내여야 합니다")
    private String content;
}
