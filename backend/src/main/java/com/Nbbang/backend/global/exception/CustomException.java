package com.Nbbang.backend.global.exception;

// 정해 둔 ErrorCode로 오류를 던지는 예외 (GlobalExceptionHandler가 받아서 응답함)
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(final ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
