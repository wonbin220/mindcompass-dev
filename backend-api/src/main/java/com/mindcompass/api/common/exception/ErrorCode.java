// 파일: ErrorCode.java
// 역할: 에러 코드 정의
// 설명: 애플리케이션에서 발생하는 모든 에러 코드를 한곳에서 관리한다

package com.mindcompass.api.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common (C)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다"),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "요청한 리소스를 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C004", "허용되지 않은 HTTP 메서드입니다"),

    // Auth (A)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "만료된 토큰입니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A004", "접근 권한이 없습니다"),

    // User (U)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 사용 중인 이메일입니다"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "U003", "비밀번호가 일치하지 않습니다"),

    // Diary (D)
    DIARY_NOT_FOUND(HttpStatus.NOT_FOUND, "D001", "일기를 찾을 수 없습니다"),
    DIARY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "D002", "해당 일기에 접근할 수 없습니다"),

    // Chat (CH)
    CHAT_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "CH001", "채팅 세션을 찾을 수 없습니다"),
    CHAT_SESSION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CH002", "해당 채팅 세션에 접근할 수 없습니다"),

    // AI (AI)
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI001", "AI 서비스를 일시적으로 사용할 수 없습니다"),
    AI_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI002", "AI 분석에 실패했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
