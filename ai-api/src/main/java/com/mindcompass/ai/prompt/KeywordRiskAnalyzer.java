// 파일: KeywordRiskAnalyzer.java
// 역할: 키워드 기반 위험도 분석
// 설명: AI 호출 없이 빠른 위험 감지 (fallback + 1차 필터)
// 중요: AI보다 먼저 실행되어 명확한 위험 키워드 즉시 감지

package com.mindcompass.ai.prompt;

import com.mindcompass.ai.dto.response.RiskScoreResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class KeywordRiskAnalyzer {

    /**
     * 고위험 키워드 (즉시 대응 필요)
     * - 자해/자살 직접 언급
     */
    private static final Set<String> HIGH_RISK_KEYWORDS = Set.of(
            "자살", "죽고싶", "죽고 싶", "죽어버릴", "자해", "손목",
            "목숨", "세상을 떠나", "더이상 살기", "더 이상 살기",
            "극단적", "끝내고 싶", "삶을 끝", "생을 마감"
    );

    /**
     * 중위험 키워드 (주의 관찰 필요)
     * - 극심한 절망감, 무가치감
     */
    private static final Set<String> MEDIUM_RISK_KEYWORDS = Set.of(
            "무가치", "쓸모없", "존재 이유", "왜 사는지", "희망이 없",
            "아무 의미", "포기하고 싶", "사라지고 싶", "없어지고 싶",
            "모든 게 끝", "버틸 수 없", "견딜 수 없"
    );

    /**
     * 키워드 기반 위험도 분석 수행
     *
     * @param content 분석할 텍스트
     * @return 위험도 분석 결과
     */
    public RiskScoreResponse analyze(String content) {
        if (content == null || content.isBlank()) {
            return RiskScoreResponse.safe();
        }

        String normalizedContent = content.toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();

        List<String> detectedHighRisk = findKeywords(normalizedContent, HIGH_RISK_KEYWORDS);
        List<String> detectedMediumRisk = findKeywords(normalizedContent, MEDIUM_RISK_KEYWORDS);

        // 고위험 키워드 발견
        if (!detectedHighRisk.isEmpty()) {
            log.warn("고위험 키워드 감지: {}", detectedHighRisk);
            return RiskScoreResponse.builder()
                    .isRisky(true)
                    .riskScore(90)
                    .riskType(determineRiskType(detectedHighRisk))
                    .detectedKeywords(detectedHighRisk)
                    .analysisMethod("KEYWORD")
                    .recommendedAction("SHOW_SAFETY_MESSAGE")
                    .build();
        }

        // 중위험 키워드 발견
        if (!detectedMediumRisk.isEmpty()) {
            log.info("중위험 키워드 감지: {}", detectedMediumRisk);
            return RiskScoreResponse.builder()
                    .isRisky(detectedMediumRisk.size() >= 2) // 2개 이상이면 위험
                    .riskScore(60)
                    .riskType(detectedMediumRisk.size() >= 2 ? "CRISIS" : null)
                    .detectedKeywords(detectedMediumRisk)
                    .analysisMethod("KEYWORD")
                    .recommendedAction(detectedMediumRisk.size() >= 2
                            ? "SHOW_SAFETY_MESSAGE" : "MONITOR")
                    .build();
        }

        // 위험 키워드 없음
        return RiskScoreResponse.safe();
    }

    private List<String> findKeywords(String content, Set<String> keywords) {
        List<String> found = new ArrayList<>();
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                found.add(keyword);
            }
        }
        return found;
    }

    private String determineRiskType(List<String> keywords) {
        for (String keyword : keywords) {
            if (keyword.contains("자살") || keyword.contains("목숨") ||
                keyword.contains("세상을 떠나") || keyword.contains("생을 마감")) {
                return "SUICIDE";
            }
            if (keyword.contains("자해") || keyword.contains("손목")) {
                return "SELF_HARM";
            }
        }
        return "CRISIS";
    }
}
