// 파일: DiaryAnalysisService.java
// 역할: 일기 분석 서비스
// 설명: AI 분석 시도 → 실패 시 fallback 응답 반환
// 원칙: 항상 structured response 반환 (예외 throw 안 함)

package com.mindcompass.ai.service;

import com.mindcompass.ai.dto.request.AnalyzeDiaryRequest;
import com.mindcompass.ai.dto.response.AnalyzeDiaryResponse;
import com.mindcompass.ai.prompt.OpenAiPromptClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryAnalysisService {

    private final OpenAiPromptClient openAiPromptClient;

    /**
     * 일기 분석 수행
     *
     * 실행 흐름:
     * 1. OpenAI 사용 가능하면 AI 분석 시도
     * 2. AI 분석 성공하면 결과 반환
     * 3. AI 실패 또는 불가능하면 fallback 반환
     *
     * @param request 일기 분석 요청
     * @return 항상 유효한 응답 (실패해도 fallback)
     */
    public AnalyzeDiaryResponse analyze(AnalyzeDiaryRequest request) {
        log.info("일기 분석 시작: diaryId={}", request.getDiaryId());

        // AI 호출 가능 여부 확인
        if (!openAiPromptClient.isAvailable()) {
            log.info("AI 클라이언트 비활성화 상태 (dev 프로필) - fallback 반환");
            return createDevFallback(request);
        }

        // AI 분석 시도
        return openAiPromptClient.analyzeDiary(request)
                .orElseGet(() -> {
                    log.warn("AI 분석 실패 - fallback 반환: diaryId={}", request.getDiaryId());
                    return AnalyzeDiaryResponse.fallback("AI 분석 실패");
                });
    }

    /**
     * dev 프로필용 Fallback 응답
     * - 실제 분석 없이 기본값 반환
     * - 개발/테스트 시 비용 발생 안 함
     */
    private AnalyzeDiaryResponse createDevFallback(AnalyzeDiaryRequest request) {
        // 간단한 휴리스틱으로 감정 추정 (dev 환경에서의 테스트용)
        String content = request.getContent();
        String emotion = "평온";
        double score = 0.5;

        if (content != null) {
            if (content.contains("행복") || content.contains("기쁘") || content.contains("좋")) {
                emotion = "기쁨";
                score = 0.8;
            } else if (content.contains("슬프") || content.contains("우울") || content.contains("힘들")) {
                emotion = "슬픔";
                score = 0.3;
            } else if (content.contains("불안") || content.contains("걱정") || content.contains("두려")) {
                emotion = "불안";
                score = 0.35;
            } else if (content.contains("화") || content.contains("짜증") || content.contains("분노")) {
                emotion = "분노";
                score = 0.25;
            }
        }

        return AnalyzeDiaryResponse.builder()
                .primaryEmotion(emotion)
                .emotionScore(score)
                .summary("[DEV] 개발 환경 fallback 응답입니다.")
                .analyzed(false)
                .failureReason("DEV_PROFILE")
                .build();
    }
}
