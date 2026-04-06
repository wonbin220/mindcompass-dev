// 파일: InternalAiController.java
// 역할: 내부 AI 엔드포인트
// 설명: backend-api만 호출하는 내부 API
// 주의: public API가 아님 - 외부 노출 금지

package com.mindcompass.ai.controller;

import com.mindcompass.ai.dto.request.AnalyzeDiaryRequest;
import com.mindcompass.ai.dto.request.GenerateReplyRequest;
import com.mindcompass.ai.dto.request.RiskScoreRequest;
import com.mindcompass.ai.dto.response.AnalyzeDiaryResponse;
import com.mindcompass.ai.dto.response.GenerateReplyResponse;
import com.mindcompass.ai.dto.response.RiskScoreResponse;
import com.mindcompass.ai.service.ChatReplyService;
import com.mindcompass.ai.service.DiaryAnalysisService;
import com.mindcompass.ai.service.RiskScoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내부 AI API 컨트롤러
 *
 * 모든 엔드포인트는 /internal/ai/** 경로 아래에 위치
 * - 외부 노출 금지
 * - backend-api만 호출 가능
 * - 인증/인가 없음 (내부 통신이므로 네트워크 레벨에서 보호)
 */
@Slf4j
@RestController
@RequestMapping("/internal/ai")
@RequiredArgsConstructor
public class InternalAiController {

    private final DiaryAnalysisService diaryAnalysisService;
    private final RiskScoreService riskScoreService;
    private final ChatReplyService chatReplyService;

    /**
     * 일기 분석
     *
     * POST /internal/ai/analyze-diary
     *
     * 호출 시점: backend-api에서 일기 저장 후
     * 실패 정책: fallback 응답 반환 (예외 throw 안 함)
     *
     * @param request 일기 분석 요청
     * @return 일기 분석 결과 (항상 200 OK)
     */
    @PostMapping("/analyze-diary")
    public ResponseEntity<AnalyzeDiaryResponse> analyzeDiary(
            @Valid @RequestBody AnalyzeDiaryRequest request) {

        log.info("일기 분석 요청: diaryId={}", request.getDiaryId());

        AnalyzeDiaryResponse response = diaryAnalysisService.analyze(request);

        log.info("일기 분석 완료: diaryId={}, analyzed={}, emotion={}",
                request.getDiaryId(), response.getAnalyzed(), response.getPrimaryEmotion());

        return ResponseEntity.ok(response);
    }

    /**
     * 위험도 분석
     *
     * POST /internal/ai/risk-score
     *
     * 호출 시점:
     * - 채팅 메시지 수신 시
     * - 일기 저장 시 (선택적)
     *
     * 실패 정책: 키워드 분석 결과로 fallback
     *
     * @param request 위험도 분석 요청
     * @return 위험도 분석 결과 (항상 200 OK)
     */
    @PostMapping("/risk-score")
    public ResponseEntity<RiskScoreResponse> analyzeRisk(
            @Valid @RequestBody RiskScoreRequest request) {

        log.info("위험도 분석 요청: userId={}, contextType={}",
                request.getUserId(), request.getContextType());

        RiskScoreResponse response = riskScoreService.analyze(request);

        if (Boolean.TRUE.equals(response.getIsRisky())) {
            log.warn("위험 감지: userId={}, riskScore={}, riskType={}",
                    request.getUserId(), response.getRiskScore(), response.getRiskType());
        } else {
            log.info("위험도 분석 완료: userId={}, riskScore={}",
                    request.getUserId(), response.getRiskScore());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 채팅 응답 생성
     *
     * POST /internal/ai/generate-reply
     *
     * 호출 시점: backend-api에서 사용자 메시지 수신 후
     * 실패 정책: fallback 응답 반환 (예외 throw 안 함)
     *
     * @param request 응답 생성 요청
     * @return AI 응답 (항상 200 OK)
     */
    @PostMapping("/generate-reply")
    public ResponseEntity<GenerateReplyResponse> generateReply(
            @Valid @RequestBody GenerateReplyRequest request) {

        log.info("채팅 응답 생성 요청: sessionId={}, userId={}",
                request.getSessionId(), request.getUserId());

        GenerateReplyResponse response = chatReplyService.generateReply(request);

        log.info("채팅 응답 생성 완료: sessionId={}, responseType={}",
                request.getSessionId(), response.getResponseType());

        return ResponseEntity.ok(response);
    }
}
