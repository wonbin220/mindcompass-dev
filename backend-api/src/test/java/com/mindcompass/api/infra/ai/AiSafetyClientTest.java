// 파일: AiSafetyClientTest.java
// 역할: AiSafetyClient의 AI 응답 처리와 fallback 동작을 검증한다
// 호출: JUnit -> AiSafetyClient

package com.mindcompass.api.infra.ai;

import com.mindcompass.api.infra.ai.dto.SafetyCheckRequest;
import com.mindcompass.api.infra.ai.dto.SafetyCheckResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;

/**
 * AiSafetyClient는 AI 안전 확인 결과를 우선 사용하고,
 * AI를 사용할 수 없거나 실패하면 키워드 기반 fallback으로 보수적으로 처리하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AiSafetyClientTest {

    @InjectMocks
    private AiSafetyClient aiSafetyClient;

    @Mock
    private WebClient aiApiWebClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private Mono<SafetyCheckResponse> mono;

    @Test
    @DisplayName("aiApiEnabled=false + 위기 키워드(자살) 포함 → fallback: isRisky=true, riskScore=80, riskLevel=HIGH")
    void checkSafety_whenAiDisabledAndCrisisKeywordExists_thenReturnsHighRiskFallback() {
        ReflectionTestUtils.setField(aiSafetyClient, "aiApiEnabled", false);
        SafetyCheckRequest request = createRequest("요즘 너무 힘들고 자살 생각이 들어", "chat");

        SafetyCheckResponse response = aiSafetyClient.checkSafety(request);

        assertThat(response.getIsRisky()).isTrue();
        assertThat(response.getRiskScore()).isEqualTo(80);
        assertThat(response.getRiskLevel()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("aiApiEnabled=false + 위기 키워드 없음 → fallback: isRisky=false, riskScore=0, riskLevel=LOW")
    void checkSafety_whenAiDisabledAndNoCrisisKeyword_thenReturnsLowRiskFallback() {
        ReflectionTestUtils.setField(aiSafetyClient, "aiApiEnabled", false);
        SafetyCheckRequest request = createRequest("오늘은 조금 지쳤지만 괜찮아질 것 같아", "chat");

        SafetyCheckResponse response = aiSafetyClient.checkSafety(request);

        assertThat(response.getIsRisky()).isFalse();
        assertThat(response.getRiskScore()).isEqualTo(0);
        assertThat(response.getRiskLevel()).isEqualTo("LOW");
    }

    @Test
    @DisplayName("aiApiEnabled=true + AI 정상 응답 → AI 응답 그대로 반환")
    void checkSafety_whenAiEnabledAndAiReturnsResponse_thenReturnsAiResponse() {
        ReflectionTestUtils.setField(aiSafetyClient, "aiApiEnabled", true);
        SafetyCheckRequest request = createRequest("계속 불안하고 무기력해", "chat");
        SafetyCheckResponse aiResponse = SafetyCheckResponse.builder()
                .isRisky(true)
                .riskScore(60)
                .riskLevel("MEDIUM")
                .recommendedAction("MONITOR")
                .safetyMessage("주의 깊게 지켜봐 주세요.")
                .build();

        given(aiApiWebClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.onStatus(any(), any())).willReturn(responseSpec); // onStatus 체인 무시
        given(responseSpec.bodyToMono(SafetyCheckResponse.class)).willReturn(mono);
        given(mono.retryWhen(any())).willReturn(mono); // retryWhen 체인 무시
        given(mono.block()).willReturn(aiResponse);

        SafetyCheckResponse response = aiSafetyClient.checkSafety(request);

        assertThat(response.getIsRisky()).isTrue();
        assertThat(response.getRiskScore()).isEqualTo(60);
        assertThat(response.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(response.getRecommendedAction()).isEqualTo("MONITOR");
        assertThat(response.getSafetyMessage()).isEqualTo("주의 깊게 지켜봐 주세요.");
    }

    @Test
    @DisplayName("aiApiEnabled=true + AI가 null 반환 → fallback 사용")
    void checkSafety_whenAiEnabledAndAiReturnsNull_thenUsesFallback() {
        ReflectionTestUtils.setField(aiSafetyClient, "aiApiEnabled", true);
        SafetyCheckRequest request = createRequest("가끔은 그냥 죽고 싶다는 생각이 들어", "chat");

        given(aiApiWebClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
        given(responseSpec.bodyToMono(SafetyCheckResponse.class)).willReturn(mono);
        given(mono.retryWhen(any())).willReturn(mono);
        given(mono.block()).willReturn(null);

        SafetyCheckResponse response = aiSafetyClient.checkSafety(request);

        assertThat(response.getIsRisky()).isTrue();
        assertThat(response.getRiskScore()).isEqualTo(80);
        assertThat(response.getRiskLevel()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("aiApiEnabled=true + AI 예외 발생 → fallback 보수적 처리")
    void checkSafety_whenAiEnabledAndExceptionOccurs_thenUsesConservativeFallback() {
        ReflectionTestUtils.setField(aiSafetyClient, "aiApiEnabled", true);
        SafetyCheckRequest request = createRequest("오늘은 평소처럼 회사에 다녀왔어", "chat");

        given(aiApiWebClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
        given(responseSpec.bodyToMono(SafetyCheckResponse.class)).willReturn(mono);
        given(mono.retryWhen(any())).willReturn(mono);
        given(mono.block()).willThrow(new RuntimeException("연결 실패"));

        SafetyCheckResponse response = aiSafetyClient.checkSafety(request);

        assertThat(response.getIsRisky()).isFalse();
        assertThat(response.getRiskScore()).isEqualTo(0);
        assertThat(response.getRiskLevel()).isEqualTo("LOW");
    }

    private SafetyCheckRequest createRequest(String content, String context) {
        return SafetyCheckRequest.builder()
                .userId(1L)
                .content(content)
                .context(context)
                .build();
    }
}
