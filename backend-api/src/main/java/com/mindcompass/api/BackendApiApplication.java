// 파일: BackendApiApplication.java
// 역할: Spring Boot 애플리케이션 진입점
// 설명: backend-api 서버의 메인 클래스

package com.mindcompass.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApiApplication.class, args);
    }
}
