// 파일: SafetyCheckRequest.java
// 역할: 안전 확인 요청 DTO
// 설명: 사용자 입력의 위기 신호를 확인하기 위한 요청

package com.mindcompass.api.infra.ai.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SafetyCheckRequest {

    private final Long userId;
    private final String content;
    private final String context; // "chat" or "diary"
}
