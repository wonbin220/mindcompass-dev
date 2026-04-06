// 파일: ChatReplyService.java
// 역할: 채팅 응답 생성 서비스
// 설명: AI 응답 생성 시도 → 실패 시 fallback 응답 반환
// 원칙: 위기 상황에서는 고정 안전 메시지 우선

package com.mindcompass.ai.service;

import com.mindcompass.ai.dto.request.GenerateReplyRequest;
import com.mindcompass.ai.dto.request.RiskScoreRequest;
import com.mindcompass.ai.dto.response.GenerateReplyResponse;
import com.mindcompass.ai.dto.response.RiskScoreResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatReplyService {

    private final RiskScoreService riskScoreService;
    private final com.mindcompass.ai.prompt.OpenAiPromptClient openAiPromptClient;

    /**
     * 채팅 응답 생성
     *
     * 실행 흐름:
     * 1. 사용자 메시지 위험도 분석
     * 2. 고위험이면 안전 메시지 반환 (AI 호출 불필요)
     * 3. AI 사용 가능하면 AI 응답 생성
     * 4. AI 실패하면 fallback 응답 반환
     *
     * @param request 응답 생성 요청
     * @return 채팅 응답 (항상 유효한 응답)
     */
    public GenerateReplyResponse generateReply(GenerateReplyRequest request) {
        log.info("채팅 응답 생성 시작: sessionId={}, userId={}",
                request.getSessionId(), request.getUserId());

        // 1. 위험도 분석 (safety-first)
        RiskScoreResponse riskResult = riskScoreService.analyze(
                RiskScoreRequest.builder()
                        .content(request.getUserMessage())
                        .userId(request.getUserId())
                        .contextType("CHAT")
                        .build()
        );

        // 2. 고위험 감지 시 안전 메시지 반환
        if (Boolean.TRUE.equals(riskResult.getIsRisky())) {
            log.warn("고위험 상황 감지 - 안전 메시지 반환: sessionId={}, riskType={}",
                    request.getSessionId(), riskResult.getRiskType());
            return GenerateReplyResponse.safetyMessage();
        }

        // 3. AI 응답 생성 시도
        if (openAiPromptClient.isAvailable()) {
            return openAiPromptClient.generateReply(request)
                    .orElseGet(() -> {
                        log.warn("AI 응답 생성 실패 - fallback 반환");
                        return GenerateReplyResponse.fallback("AI 응답 생성 실패");
                    });
        }

        // 4. AI 불가능 - fallback 응답
        log.info("AI 클라이언트 비활성화 - fallback 응답 반환");
        return createDevFallback(request);
    }

    /**
     * dev 프로필용 Fallback 응답
     */
    private GenerateReplyResponse createDevFallback(GenerateReplyRequest request) {
        String userMessage = request.getUserMessage();
        String reply;
        String emotion = null;

        // 간단한 휴리스틱 응답 (dev 테스트용)
        if (userMessage.contains("?") || userMessage.contains("어떻게")) {
            reply = "[DEV] 좋은 질문이네요. 조금 더 이야기해 주시겠어요?";
        } else if (userMessage.contains("힘들") || userMessage.contains("슬프")) {
            reply = "[DEV] 지금 많이 힘드시군요. 그 마음 충분히 이해해요.";
            emotion = "슬픔";
        } else if (userMessage.contains("화") || userMessage.contains("짜증")) {
            reply = "[DEV] 그런 상황이면 화가 날 수 있어요. 무슨 일이 있었는지 들려주세요.";
            emotion = "분노";
        } else {
            reply = "[DEV] 네, 이야기 듣고 있어요. 계속 말씀해 주세요.";
        }

        return GenerateReplyResponse.builder()
                .reply(reply)
                .detectedEmotion(emotion)
                .generated(false)
                .responseType("FALLBACK")
                .failureReason("DEV_PROFILE")
                .build();
    }
}
