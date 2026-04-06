// 파일: HealthController.java
// 역할: 서버 상태 확인 엔드포인트
// 설명: backend-api가 ai-api 상태 확인 시 사용

package com.mindcompass.ai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final Environment environment;

    @Value("${spring.ai.openai.api-key:NOT_SET}")
    private String apiKeyStatus;

    /**
     * 서버 상태 확인
     *
     * GET /health
     *
     * 용도:
     * - backend-api → ai-api 연결 확인
     * - 로드밸런서 헬스체크
     * - 배포 후 상태 확인
     *
     * @return 서버 상태 정보
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();

        response.put("status", "UP");
        response.put("service", "ai-api");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("profiles", Arrays.asList(environment.getActiveProfiles()));

        // AI 사용 가능 여부
        boolean aiEnabled = Arrays.asList(environment.getActiveProfiles()).contains("manual");
        response.put("aiEnabled", aiEnabled);

        if (aiEnabled) {
            boolean hasApiKey = !"NOT_SET".equals(apiKeyStatus) && !apiKeyStatus.isBlank();
            response.put("openaiConfigured", hasApiKey);
        }

        return ResponseEntity.ok(response);
    }
}
