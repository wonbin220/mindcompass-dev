// 파일: AiApiApplication.java
// 역할: ai-api Spring Boot 메인 클래스
// 설명: 내부 AI 오케스트레이션 서버 진입점

package com.mindcompass.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AiApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApiApplication.class, args);
    }
}
