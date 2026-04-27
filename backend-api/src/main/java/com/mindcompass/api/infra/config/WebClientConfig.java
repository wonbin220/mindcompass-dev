// 파일: WebClientConfig.java
// 역할: WebClient 빈 설정
// 설명: ai-api 호출용 WebClient를 구성한다

package com.mindcompass.api.infra.config;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${ai.api.base-url}")
    private String aiApiBaseUrl;

    @Value("${ai.api.timeout-ms:5000}")
    private int timeoutMs;

    @Value("${ai.api.retry-count:2}")
    private int retryCount;

    @Bean
    public WebClient aiApiWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
                .responseTimeout(Duration.ofMillis(timeoutMs));

        return WebClient.builder()
                .baseUrl(aiApiBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    // 요청 로깅 필터
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("[AI-API 요청] {} {}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    // 응답 로깅 필터
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.debug("[AI-API 응답] status={}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
}
