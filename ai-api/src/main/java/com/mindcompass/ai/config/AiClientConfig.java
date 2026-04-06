// 파일: AiClientConfig.java
// 역할: Spring AI ChatClient 설정
// 설명: 프로필에 따라 실제 OpenAI 또는 Mock 클라이언트 주입

package com.mindcompass.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class AiClientConfig {

    /**
     * manual 프로필: 실제 OpenAI ChatClient 사용
     * - 실제 API 호출이 발생하므로 비용 주의
     * - application-manual.yml에 API 키 설정 필요
     */
    @Bean
    @Profile("manual")
    public ChatClient realChatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    /**
     * dev 프로필: ChatClient Bean 없음
     * - 서비스에서 fallback 로직만 사용
     * - OpenAI 호출 없이 개발 가능
     */
    // dev 프로필에서는 ChatClient Bean을 생성하지 않음
    // 서비스에서 Optional<ChatClient>로 주입받아 처리
}
