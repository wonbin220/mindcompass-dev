// 파일: AiDiaryAnalysisClient.java
// 역할: 일기 분석용 AI API 클라이언트
// 설명: ai-api의 일기 분석 엔드포인트를 호출한다
// 핵심 원칙: AI 실패가 일기 저장을 막으면 안 된다

package com.mindcompass.api.infra.ai;

import com.mindcompass.api.infra.ai.dto.DiaryAnalysisRequest;
import com.mindcompass.api.infra.ai.dto.DiaryAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiDiaryAnalysisClient {

    private final WebClient aiApiWebClient;

    @Value("${ai.api.enabled:true}")
    private boolean aiApiEnabled;

    /**
     * 일기 내용을 분석한다.
     *
     * AI 호출이 실패해도 Optional.empty()를 반환한다.
     * 일기 저장 흐름은 AI 실패와 무관하게 진행되어야 한다.
     *
     * @param request 분석할 일기 정보
     * @return 분석 결과 (AI 실패 시 Optional.empty())
     */
    public Optional<DiaryAnalysisResponse> analyze(DiaryAnalysisRequest request) {
        if (!aiApiEnabled) {
            log.info("AI API 비활성화 상태, 분석 건너뜀: diaryId={}", request.getDiaryId());
            return Optional.empty();
        }

        try {
            DiaryAnalysisResponse response = aiApiWebClient.post()
                    .uri("/api/v1/analyze/diary")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(DiaryAnalysisResponse.class)
                    .block(); // 동기 호출 (필요시 비동기로 변경 가능)

            log.info("일기 분석 완료: diaryId={}, emotion={}",
                    request.getDiaryId(),
                    response != null ? response.getPrimaryEmotion() : "null");

            return Optional.ofNullable(response);

        } catch (WebClientResponseException e) {
            log.warn("AI API 응답 오류: status={}, body={}, diaryId={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), request.getDiaryId());
            return Optional.empty();

        } catch (Exception e) {
            log.warn("AI API 호출 실패: diaryId={}, error={}",
                    request.getDiaryId(), e.getMessage());
            return Optional.empty();
        }
    }
}
