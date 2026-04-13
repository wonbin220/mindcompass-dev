// 파일: AiSafetyClient.java
// 역할: 안전 확인용 AI API 클라이언트
// 설명: 사용자 입력의 위기 신호를 감지한다
// 핵심 원칙: Safety-first, AI 실패 시 보수적으로 처리

package com.mindcompass.api.infra.ai;

import com.mindcompass.api.infra.ai.dto.SafetyCheckRequest;
import com.mindcompass.api.infra.ai.dto.SafetyCheckResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiSafetyClient {

    private final WebClient aiApiWebClient;

    @Value("${ai.api.enabled:true}")
    private boolean aiApiEnabled;

    // 위기 키워드 (AI 실패 시 fallback용)
    private static final List<String> CRISIS_KEYWORDS = List.of(
            "죽고 싶", "자살", "자해", "목숨", "끝내고 싶",
            "살기 싫", "없어지고 싶", "사라지고 싶"
    );

    // 기본 안전 메시지
    private static final String DEFAULT_SAFETY_MESSAGE =
            "지금 많이 힘드시군요. 전문 상담이 도움이 될 수 있어요.\n" +
            "자살예방상담전화 1393\n" +
            "정신건강위기상담전화 1577-0199";

    /**
     * 사용자 입력의 위기 신호를 확인한다.
     *
     * AI 호출이 실패하면 키워드 기반 fallback을 사용한다.
     * 안전은 AI보다 우선이므로, 실패 시에도 보수적으로 처리한다.
     */
    public SafetyCheckResponse checkSafety(SafetyCheckRequest request) {
        if (!aiApiEnabled) {
            log.info("AI API 비활성화 상태, 키워드 기반 확인 수행");
            return fallbackSafetyCheck(request.getContent());
        }

        try {
            SafetyCheckResponse response = aiApiWebClient.post()
                    .uri("/internal/ai/risk-score")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(SafetyCheckResponse.class)
                    .block();

            if (response != null) {
                log.info("안전 확인 완료: userId={}, isRisky={}, riskLevel={}",
                        request.getUserId(), response.getIsRisky(), response.getRiskLevel());
                return response;
            }

            return fallbackSafetyCheck(request.getContent());

        } catch (Exception e) {
            log.warn("AI 안전 확인 실패, fallback 사용: userId={}, error={}",
                    request.getUserId(), e.getMessage());
            return fallbackSafetyCheck(request.getContent());
        }
    }

    /**
     * AI 실패 시 키워드 기반 fallback 안전 확인
     */
    private SafetyCheckResponse fallbackSafetyCheck(String content) {
        boolean hasKeyword = CRISIS_KEYWORDS.stream()
                .anyMatch(keyword -> content.contains(keyword));

        if (hasKeyword) {
            log.warn("키워드 기반 위기 감지됨");
            return SafetyCheckResponse.builder()
                    .isRisky(true)
                    .riskScore(80)
                    .riskLevel("HIGH")
                    .recommendedAction("SHOW_SAFETY_MESSAGE")
                    .safetyMessage(DEFAULT_SAFETY_MESSAGE)
                    .build();
        }

        return SafetyCheckResponse.builder()
                .isRisky(false)
                .riskScore(0)
                .riskLevel("LOW")
                .build();
    }
}
