// 파일: JpaConfig.java
// 역할: JPA 관련 설정
// 설명: Auditing 활성화 (createdAt, updatedAt 자동 관리)

package com.mindcompass.api.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaConfig {
    // @CreatedDate, @LastModifiedDate 자동 주입을 위해 필요
}
