// 파일: CurrentUser.java
// 역할: 현재 인증된 사용자 ID를 주입받는 어노테이션
// 설명: 컨트롤러에서 @CurrentUser Long userId 형태로 사용한다

package com.mindcompass.api.common.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
}
