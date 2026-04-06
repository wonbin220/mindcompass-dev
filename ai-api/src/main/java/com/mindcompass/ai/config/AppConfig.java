// 파일: AppConfig.java
// 역할: 애플리케이션 공통 설정
// 설명: ObjectMapper, RestTemplate 등 공통 Bean 설정

package com.mindcompass.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * ai-api-fastapi 호출용 RestTemplate
     * - 추후 FastAPI 모델 서버 연동 시 사용
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
