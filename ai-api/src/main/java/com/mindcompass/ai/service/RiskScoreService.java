// 파일: RiskScoreService.java
// 역할: 위험도 분석 서비스
// 설명: 키워드 분석(1차) → AI 분석(2차, optional) → 결과 병합
// 원칙: 키워드 분석은 항상 수행, AI는 보조적

package com.mindcompass.ai.service;

import com.mindcompass.ai.dto.request.RiskScoreRequest;
import com.mindcompass.ai.dto.response.RiskScoreResponse;
import com.mindcompass.ai.prompt.KeywordRiskAnalyzer;
import com.mindcompass.ai.prompt.OpenAiPromptClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskScoreService {

    private final KeywordRiskAnalyzer keywordRiskAnalyzer;
    private final OpenAiPromptClient openAiPromptClient;

    /**
     * 위험도 분석 수행
     *
     * 실행 흐름:
     * 1. 키워드 기반 분석 (항상 수행, 빠름)
     * 2. 고위험 키워드 발견 시 즉시 반환 (AI 호출 불필요)
     * 3. AI 사용 가능하고 키워드 분석이 불확실하면 AI 분석 추가
     * 4. 최종 결과 반환
     *
     * @param request 위험도 분석 요청
     * @return 위험도 분석 결과
     */
    public RiskScoreResponse analyze(RiskScoreRequest request) {
        log.info("위험도 분석 시작: userId={}, contextType={}",
                request.getUserId(), request.getContextType());

        // 1. 키워드 기반 분석 (항상 수행)
        RiskScoreResponse keywordResult = keywordRiskAnalyzer.analyze(request.getContent());

        // 2. 고위험 키워드 발견 시 즉시 반환
        if (Boolean.TRUE.equals(keywordResult.getIsRisky()) &&
            keywordResult.getRiskScore() >= 80) {
            log.warn("고위험 키워드 감지됨 - 즉시 반환: riskType={}",
                    keywordResult.getRiskType());
            return keywordResult;
        }

        // 3. AI 사용 가능하면 추가 분석
        if (openAiPromptClient.isAvailable()) {
            return openAiPromptClient.analyzeRisk(request)
                    .map(aiResult -> mergeResults(keywordResult, aiResult))
                    .orElseGet(() -> {
                        log.info("AI 분석 실패 - 키워드 분석 결과 사용");
                        return keywordResult;
                    });
        }

        // 4. AI 불가능하면 키워드 분석 결과 반환
        log.info("AI 클라이언트 비활성화 - 키워드 분석 결과 사용");
        return keywordResult;
    }

    /**
     * 키워드 분석 + AI 분석 결과 병합
     * - 더 높은 위험도를 채택
     * - 키워드 분석에서 감지된 키워드 보존
     */
    private RiskScoreResponse mergeResults(RiskScoreResponse keyword, RiskScoreResponse ai) {
        // 더 높은 위험도 채택
        boolean isRisky = keyword.getIsRisky() || ai.getIsRisky();
        int riskScore = Math.max(
                keyword.getRiskScore() != null ? keyword.getRiskScore() : 0,
                ai.getRiskScore() != null ? ai.getRiskScore() : 0
        );

        // 위험 유형 결정 (키워드 > AI)
        String riskType = keyword.getRiskType() != null
                ? keyword.getRiskType()
                : ai.getRiskType();

        return RiskScoreResponse.builder()
                .isRisky(isRisky)
                .riskScore(riskScore)
                .riskType(riskType)
                .detectedKeywords(keyword.getDetectedKeywords()) // 키워드는 키워드 분석에서
                .analysisMethod("HYBRID") // 키워드 + AI
                .recommendedAction(isRisky ? "SHOW_SAFETY_MESSAGE" : "NONE")
                .build();
    }
}
