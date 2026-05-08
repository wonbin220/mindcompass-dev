// 파일: FastApiEmotionClient.java
// 역할: ai-api-fastapi 감정분류 엔드포인트 호출 클라이언트
// 호출: DiaryAnalysisService → POST /internal/model/emotion-classify

package com.mindcompass.ai.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class FastApiEmotionClient {

    private final RestTemplate restTemplate;

    @Value("${fastapi.emotion.base-url}")
    private String baseUrl;

    public FastApiEmotionClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<EmotionResult> classify(String text) {
        try {
            var request = new ClassifyRequest(text, 3, null);
            var response = restTemplate.postForObject(
                    baseUrl + "/internal/model/emotion-classify",
                    request,
                    ClassifyResponse.class
            );

            if (response == null) return Optional.empty();

            log.info("감정분류 완료: emotion={}, confidence={}",
                    response.primaryEmotion(), response.confidence());

            return Optional.of(new EmotionResult(
                    response.primaryEmotion(),
                    response.confidence(),
                    response.fallbackUsed()
            ));

        } catch (Exception e) {
            log.warn("FastAPI 감정분류 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }
    // DTO records (내부용)
    public record ClassifyRequest(String text, Integer returnTopK, String sessionId) {}

    public record ClassifyResponse(
            String primaryEmotion,
            double confidence,
            List<String> emotionTags,
            String modelName,
            String modelVersion,
            boolean fallbackUsed,
            String fallbackReason
    ) {}

    public record EmotionResult(
            String primaryEmotion,
            double confidence,
            boolean fallbackUsed
    ) {}
}
