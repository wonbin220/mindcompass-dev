// 파일: ProfileLogger.java
// 역할: 활성 프로필 로깅
// 설명: 서버 시작 시 어떤 프로필로 실행되는지 명확히 알려줌

package com.mindcompass.ai.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
public class ProfileLogger {

    private final Environment environment;

    @Value("${spring.ai.openai.api-key:NOT_SET}")
    private String apiKeyStatus;

    public ProfileLogger(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void logActiveProfiles() {
        String[] activeProfiles = environment.getActiveProfiles();

        log.info("========================================");
        log.info("AI-API Server Starting");
        log.info("Active Profiles: {}", Arrays.toString(activeProfiles));

        if (Arrays.asList(activeProfiles).contains("manual")) {
            boolean hasApiKey = !"NOT_SET".equals(apiKeyStatus) && !apiKeyStatus.isBlank();
            log.info("Mode: MANUAL (Real OpenAI calls)");
            log.info("OpenAI API Key: {}", hasApiKey ? "CONFIGURED" : "NOT CONFIGURED - Will fail!");
            if (!hasApiKey) {
                log.warn("⚠️  OpenAI API Key가 설정되지 않았습니다!");
                log.warn("⚠️  application-manual.yml에 spring.ai.openai.api-key를 설정하세요.");
            }
        } else {
            log.info("Mode: DEV (Fallback only, zero-cost)");
            log.info("OpenAI calls will NOT be made");
        }

        log.info("========================================");
    }
}
