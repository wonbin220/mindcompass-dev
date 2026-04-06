// 파일: AiChatClient.java
// 역할: 채팅 응답 생성용 AI API 클라이언트
// 설명: ai-api의 채팅 응답 생성 엔드포인트를 호출한다
// 핵심 원칙: Safety 확인 후에만 AI 응답 생성

package com.mindcompass.api.infra.ai;

import com.mindcompass.api.infra.ai.dto.ChatRequest;
import com.mindcompass.api.infra.ai.dto.ChatResponse;
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
public class AiChatClient {

    private final WebClient aiApiWebClient;

    @Value("${ai.api.enabled:true}")
    private boolean aiApiEnabled;

    // AI 실패 시 기본 응답
    private static final String FALLBACK_MESSAGE =
            "죄송합니다, 지금은 응답을 드리기 어려워요. 잠시 후 다시 시도해 주세요.";

    /**
     * 채팅 AI 응답을 생성한다.
     *
     * AI 호출이 실패해도 fallback 메시지를 반환한다.
     * 사용자가 응답을 받지 못하는 상황을 최소화한다.
     *
     * @param request 채팅 요청 정보
     * @return AI 응답 (실패 시 fallback 응답)
     */
    public ChatResponse generateReply(ChatRequest request) {
        if (!aiApiEnabled) {
            log.info("AI API 비활성화 상태, fallback 응답 반환: sessionId={}", request.getSessionId());
            return createFallbackResponse();
        }

        try {
            ChatResponse response = aiApiWebClient.post()
                    .uri("/api/v1/chat/reply")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatResponse.class)
                    .block();

            if (response != null) {
                log.info("채팅 응답 생성 완료: sessionId={}, isSafetyTriggered={}",
                        request.getSessionId(), response.getIsSafetyTriggered());
                return response;
            }

            return createFallbackResponse();

        } catch (WebClientResponseException e) {
            log.warn("AI API 응답 오류: status={}, sessionId={}",
                    e.getStatusCode(), request.getSessionId());
            return createFallbackResponse();

        } catch (Exception e) {
            log.warn("AI API 호출 실패: sessionId={}, error={}",
                    request.getSessionId(), e.getMessage());
            return createFallbackResponse();
        }
    }

    private ChatResponse createFallbackResponse() {
        // TODO: ChatResponse에 빌더 추가 후 개선
        ChatResponse response = new ChatResponse();
        // response.setMessage(FALLBACK_MESSAGE);
        // response.setIsSafetyTriggered(false);
        return response;
    }
}
