// 파일: OpenAiPromptClient.java
// 역할: OpenAI API 호출 클라이언트
// 설명: Spring AI ChatClient를 사용한 실제 LLM 호출
// 프로필: manual 프로필에서만 활성화

package com.mindcompass.ai.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcompass.ai.dto.request.AnalyzeDiaryRequest;
import com.mindcompass.ai.dto.request.GenerateReplyRequest;
import com.mindcompass.ai.dto.request.RiskScoreRequest;
import com.mindcompass.ai.dto.response.AnalyzeDiaryResponse;
import com.mindcompass.ai.dto.response.GenerateReplyResponse;
import com.mindcompass.ai.dto.response.RiskScoreResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class OpenAiPromptClient {

    private final Optional<ChatClient> chatClient;
    private final ObjectMapper objectMapper;

    public OpenAiPromptClient(Optional<ChatClient> chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    /**
     * ChatClient 사용 가능 여부 확인
     * - dev 프로필에서는 false 반환
     */
    public boolean isAvailable() {
        return chatClient.isPresent();
    }

    /**
     * 일기 분석 AI 호출
     */
    public Optional<AnalyzeDiaryResponse> analyzeDiary(AnalyzeDiaryRequest request) {
        if (!isAvailable()) {
            log.debug("ChatClient not available, skipping AI call");
            return Optional.empty();
        }

        try {
            String userPrompt = String.format(
                    PromptTemplates.DIARY_ANALYSIS_USER,
                    request.getTitle() != null ? request.getTitle() : "(제목 없음)",
                    request.getContent()
            );

            List<Message> messages = List.of(
                    new SystemMessage(PromptTemplates.DIARY_ANALYSIS_SYSTEM),
                    new UserMessage(userPrompt)
            );

            String response = chatClient.get()
                    .prompt(new Prompt(messages))
                    .call()
                    .content();

            return parseAnalyzeDiaryResponse(response);

        } catch (Exception e) {
            log.error("일기 분석 AI 호출 실패: diaryId={}, error={}", request.getDiaryId(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 위험도 분석 AI 호출
     */
    public Optional<RiskScoreResponse> analyzeRisk(RiskScoreRequest request) {
        if (!isAvailable()) {
            log.debug("ChatClient not available, skipping AI call");
            return Optional.empty();
        }

        try {
            String userPrompt = String.format(
                    PromptTemplates.RISK_ANALYSIS_USER,
                    request.getContent()
            );

            List<Message> messages = List.of(
                    new SystemMessage(PromptTemplates.RISK_ANALYSIS_SYSTEM),
                    new UserMessage(userPrompt)
            );

            String response = chatClient.get()
                    .prompt(new Prompt(messages))
                    .call()
                    .content();

            return parseRiskScoreResponse(response);

        } catch (Exception e) {
            log.error("위험도 분석 AI 호출 실패: error={}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 채팅 응답 생성 AI 호출
     */
    public Optional<GenerateReplyResponse> generateReply(GenerateReplyRequest request) {
        if (!isAvailable()) {
            log.debug("ChatClient not available, skipping AI call");
            return Optional.empty();
        }

        try {
            List<Message> messages = buildChatMessages(request);

            String response = chatClient.get()
                    .prompt(new Prompt(messages))
                    .call()
                    .content();

            return parseGenerateReplyResponse(response);

        } catch (Exception e) {
            log.error("채팅 응답 생성 AI 호출 실패: sessionId={}, error={}",
                    request.getSessionId(), e.getMessage());
            return Optional.empty();
        }
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    private List<Message> buildChatMessages(GenerateReplyRequest request) {
        List<Message> messages = new ArrayList<>();

        // 시스템 메시지 (안전 모드 여부에 따라 다름)
        String systemPrompt = Boolean.TRUE.equals(request.getSafetyMode())
                ? PromptTemplates.CHAT_SYSTEM_SAFETY_MODE
                : PromptTemplates.CHAT_SYSTEM;
        messages.add(new SystemMessage(systemPrompt));

        // 대화 히스토리 추가
        if (request.getConversationHistory() != null) {
            for (GenerateReplyRequest.ChatMessage msg : request.getConversationHistory()) {
                if ("USER".equals(msg.getRole())) {
                    messages.add(new UserMessage(msg.getContent()));
                } else if ("ASSISTANT".equals(msg.getRole())) {
                    messages.add(new AssistantMessage(msg.getContent()));
                }
            }
        }

        // 현재 사용자 메시지
        messages.add(new UserMessage(request.getUserMessage()));

        return messages;
    }

    private Optional<AnalyzeDiaryResponse> parseAnalyzeDiaryResponse(String response) {
        try {
            String json = extractJson(response);
            JsonNode node = objectMapper.readTree(json);

            return Optional.of(AnalyzeDiaryResponse.success(
                    node.path("primaryEmotion").asText("미분석"),
                    node.path("emotionScore").asDouble(0.5),
                    node.path("summary").asText("")
            ));
        } catch (JsonProcessingException e) {
            log.warn("일기 분석 응답 파싱 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<RiskScoreResponse> parseRiskScoreResponse(String response) {
        try {
            String json = extractJson(response);
            JsonNode node = objectMapper.readTree(json);

            return Optional.of(RiskScoreResponse.builder()
                    .isRisky(node.path("isRisky").asBoolean(false))
                    .riskScore(node.path("riskScore").asInt(0))
                    .riskType(node.path("riskType").isNull() ? null : node.path("riskType").asText())
                    .detectedKeywords(List.of())
                    .analysisMethod("AI")
                    .recommendedAction(node.path("isRisky").asBoolean() ? "SHOW_SAFETY_MESSAGE" : "NONE")
                    .build());
        } catch (JsonProcessingException e) {
            log.warn("위험도 분석 응답 파싱 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<GenerateReplyResponse> parseGenerateReplyResponse(String response) {
        try {
            String json = extractJson(response);
            JsonNode node = objectMapper.readTree(json);

            return Optional.of(GenerateReplyResponse.success(
                    node.path("reply").asText(""),
                    node.path("detectedEmotion").isNull() ? null : node.path("detectedEmotion").asText()
            ));
        } catch (JsonProcessingException e) {
            log.warn("채팅 응답 파싱 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * LLM 응답에서 JSON 부분만 추출
     * - 응답이 markdown code block으로 감싸진 경우 처리
     */
    private String extractJson(String response) {
        if (response == null) return "{}";

        String trimmed = response.trim();

        // ```json ... ``` 형태 처리
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (start > 0 && end > start) {
                return trimmed.substring(start, end).trim();
            }
        }

        // { ... } 형태 추출
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }

        return trimmed;
    }
}
