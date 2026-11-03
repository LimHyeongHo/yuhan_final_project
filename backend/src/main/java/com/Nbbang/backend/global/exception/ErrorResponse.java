package com.Nbbang.backend.global.exception;

import lombok.Getter;

import java.time.LocalDateTime;

// 오류가 나면 프론트로 보내는 공통 응답 형식
@Getter
public class ErrorResponse {

    private final boolean success;  // 에러 응답이므로 항상 false
    private final String code;      // ErrorCode 이름 (예: "MEMBER_DUPLICATE_EMAIL")
    private final String message;   // 사용자에게 보여줄 메시지
    private final LocalDateTime timestamp;
    private final String path;

    private ErrorResponse(final ErrorCode errorCode, final String path) {
        this.success = false;
        this.code = errorCode.name();
        this.message = errorCode.getMessage();
        this.timestamp = LocalDateTime.now();
        this.path = path;
    }

    // 생성자는 막아 두고 이 메서드로만 만들게 함
    public static ErrorResponse of(final ErrorCode errorCode, final String path) {
        return new ErrorResponse(errorCode, path);
    }
}
