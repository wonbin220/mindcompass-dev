// 파일: DiaryAnalysisService.java
// 역할: 일기 분석 서비스
// 설명: AI 분석 시도 → 실패 시 fallback 응답 반환
// 원칙: 항상 structured response 반환 (예외 throw 안 함)

package com.mindcompass.ai.service;

import com.mindcompass.ai.client.FastApiEmotionClient;
import com.mindcompass.ai.dto.request.AnalyzeDiaryRequest;
import com.mindcompass.ai.dto.request.RiskScoreRequest;
import com.mindcompass.ai.dto.response.AnalyzeDiaryResponse;
import com.mindcompass.ai.dto.response.RiskScoreResponse;
import com.mindcompass.ai.prompt.OpenAiPromptClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryAnalysisService {

    private final OpenAiPromptClient openAiPromptClient;
    private final FastApiEmotionClient fastApiEmotionClient;
    private final RiskScoreService riskScoreService;

    private static final Map<String, String> EMOTION_KO;

    static {
        EMOTION_KO = new HashMap<>();
        EMOTION_KO.put("happy",   "기쁨");
        EMOTION_KO.put("calm",    "평온");
        EMOTION_KO.put("anxious", "불안");
        EMOTION_KO.put("sad",     "슬픔");
        EMOTION_KO.put("angry",   "분노");
        EMOTION_KO.put("tired",   "피곤");
    }

    /**
     * 일기 분석 수행
     *
     * 실행 흐름:
     * 1. FastAPI 감정분류 먼저 시도 (학습된 모델)
     * 2. OpenAI 가능하면 요약 생성 추가 (감정은 FastAPI 결과 우선)
     * 3. FastAPI 실패 시 OpenAI 결과만 사용
     * 4. 모두 실패 시 keyword fallback
     * 5. 키워드 기반 위험도 분석 항상 수행 (riskLevel 포함)
     *
     * @param request 일기 분석 요청
     * @return 항상 유효한 응답 (실패해도 fallback)
     */
    public AnalyzeDiaryResponse analyze(AnalyzeDiaryRequest request) {
        log.info("일기 분석 시작: diaryId={}", request.getDiaryId());

        String text = (request.getTitle() != null ? request.getTitle() + " " : "")
                + request.getContent();

        // 1. FastAPI 감정분류
        Optional<FastApiEmotionClient.EmotionResult> emotionResult =
                fastApiEmotionClient.classify(text);

        // 2. 감정 + 요약 결과 조합
        AnalyzeDiaryResponse base;
        if (openAiPromptClient.isAvailable()) {
            base = openAiPromptClient.analyzeDiary(request)
                    .map(aiResult -> {
                        if (emotionResult.isPresent()) {
                            return AnalyzeDiaryResponse.builder()
                                    .primaryEmotion(emotionResult.get().primaryEmotion())
                                    .emotionScore(emotionResult.get().confidence())
                                    .summary(aiResult.getSummary())
                                    .analyzed(true)
                                    .build();
                        }
                        return aiResult;
                    })
                    .orElseGet(() -> buildFromEmotion(emotionResult, request));
        } else {
            base = buildFromEmotion(emotionResult, request);
        }

        // 3. 위험도 분석 (키워드 기반, 항상 수행)
        String riskLevel = computeRiskLevel(text, request.getUserId());

        return AnalyzeDiaryResponse.builder()
                .primaryEmotion(base.getPrimaryEmotion())
                .emotionScore(base.getEmotionScore())
                .summary(base.getSummary())
                .analyzed(base.getAnalyzed())
                .failureReason(base.getFailureReason())
                .riskLevel(riskLevel)
                .build();
    }

    private String computeRiskLevel(String text, Long userId) {
        try {
            RiskScoreRequest riskRequest = RiskScoreRequest.builder()
                    .content(text)
                    .userId(userId)
                    .contextType("DIARY")
                    .build();
            RiskScoreResponse risk = riskScoreService.analyze(riskRequest);
            int score = risk.getRiskScore() != null ? risk.getRiskScore() : 0;
            if (score >= 80) return "HIGH";
            if (score >= 40) return "MEDIUM";
            return "LOW";
        } catch (Exception e) {
            log.warn("위험도 분석 실패 (LOW 기본값 사용): {}", e.getMessage());
            return "LOW";
        }
    }

    private AnalyzeDiaryResponse buildFromEmotion(
            Optional<FastApiEmotionClient.EmotionResult> emotionResult,
            AnalyzeDiaryRequest request) {

        if (emotionResult.isPresent()) {
            FastApiEmotionClient.EmotionResult e = emotionResult.get();
            String emotionKo = EMOTION_KO.getOrDefault(e.primaryEmotion(), e.primaryEmotion());
            int confidencePct = (int) (e.confidence() * 100);
            String summary = String.format(
                    "오늘 일기에서 '%s' 감정이 감지되었습니다. (신뢰도 %d%%)", emotionKo, confidencePct);

            log.info("FastAPI 감정분류 결과 사용: emotion={}", e.primaryEmotion());
            return AnalyzeDiaryResponse.builder()
                    .primaryEmotion(e.primaryEmotion())
                    .emotionScore(e.confidence())
                    .summary(summary)
                    .analyzed(!e.fallbackUsed())
                    .build();
        }

        log.info("모든 AI 실패 - keyword fallback: diaryId={}", request.getDiaryId());
        return createDevFallback(request);
    }

    /**
     * dev 프로필용 Fallback 응답
     * - 실제 분석 없이 기본값 반환
     * - 개발/테스트 시 비용 발생 안 함
     */
    private AnalyzeDiaryResponse createDevFallback(AnalyzeDiaryRequest request) {
        String content = request.getContent();
        String emotion = "평온";
        double score = 0.5;

        if (content != null) {
            if (content.contains("행복") || content.contains("기쁘") || content.contains("좋")) {
                emotion = "happy"; score = 0.8;
            } else if (content.contains("슬프") || content.contains("우울") || content.contains("힘들")) {
                emotion = "sad"; score = 0.3;
            } else if (content.contains("불안") || content.contains("걱정") || content.contains("두려")) {
                emotion = "anxious"; score = 0.35;
            } else if (content.contains("화") || content.contains("짜증") || content.contains("분노")) {
                emotion = "angry"; score = 0.25;
            }
        }

        String emotionKo = EMOTION_KO.getOrDefault(emotion, emotion);
        return AnalyzeDiaryResponse.builder()
                .primaryEmotion(emotion)
                .emotionScore(score)
                .summary(emotionKo + " 감정이 느껴지는 하루였네요.")
                .analyzed(false)
                .failureReason("DEV_PROFILE")
                .build();
    }
}
