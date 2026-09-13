package com.Nbbang.backend.global.exception;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 에러 발생 시 프론트엔드로 전달되는 공통 응답 형식입니다.
 *
 * 응답 예시:
 * {
 *   "success": false,
 *   "code": "MEMBER_DUPLICATE_EMAIL",
 *   "message": "이미 사용 중인 이메일입니다",
 *   "timestamp": "2026-09-13T10:15:30",
 *   "path": "/api/members/signup"
 * }
 *
 * 사용법:
 * return ResponseEntity
 *     .status(errorCode.getStatus())
 *     .body(ErrorResponse.of(errorCode, request.getRequestURI()));
 */
@Getter
public class ErrorResponse {

    private final boolean success;  // 에러 응답이므로 항상 false
    private final String code;      // ErrorCode enum 이름 (예: "MEMBER_DUPLICATE_EMAIL")
    private final String message;   // 사용자에게 표시할 메시지
    private final LocalDateTime timestamp; // 에러 발생 시각
    private final String path;      // 요청 경로 (예: "/api/members/signup")

    // [수정][fix/exception-nfr] NFR-001: 모든 에러 응답에 timestamp/path 포함하도록 필드 추가
    private ErrorResponse(final ErrorCode errorCode, final String path) {
        this.success = false;
        this.code = errorCode.name();
        this.message = errorCode.getMessage();
        this.timestamp = LocalDateTime.now();
        this.path = path;
    }

    /**
     * ErrorResponse 생성 메서드
     * new 키워드 대신 이 메서드를 통해서만 생성할 수 있습니다.
     *
     * 사용 예시:
     * ErrorResponse.of(ErrorCode.MEMBER_DUPLICATE_EMAIL, "/api/members/signup")
     */
    public static ErrorResponse of(final ErrorCode errorCode, final String path) {
        return new ErrorResponse(errorCode, path);
    }
}
