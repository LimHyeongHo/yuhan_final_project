package com.Nbbang.backend.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 전역 예외 처리 핸들러.
 * 어디서든 예외가 발생하면 자동으로 잡아서 ErrorResponse 형태로 프론트에 응답합니다.
 * 흐름: throw new CustomException(ErrorCode.XXX) → 여기서 캐치 → ErrorResponse 반환
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * CustomException 처리. 팀원들이 throw new CustomException(ErrorCode.XXX) 로 던진 에러를 잡습니다.
     * 예시: throw new CustomException(ErrorCode.DUPLICATE_EMAIL)
     * 응답: { "success": false, "code": "DUPLICATE_EMAIL", "message": "이미 사용 중인 이메일입니다", "timestamp": ..., "path": ... }
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(final CustomException e, final HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(HttpStatus.valueOf(errorCode.getStatus()))
                .body(ErrorResponse.of(errorCode, request.getRequestURI()));
    }

    /**
     * Validation 예외 처리. 컨트롤러 파라미터에 @Valid 붙였을 때 검증 실패하면 여기서 잡습니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(final MethodArgumentNotValidException e, final HttpServletRequest request) {
        // [수정][fix/exception-nfr] NFR-001: 400 하드코딩 제거 — 다른 핸들러와 동일하게 ErrorCode의
        // status를 그대로 따라가도록 통일 (VALIDATION_FAILED의 status가 바뀌어도 같이 반영되게)
        return ResponseEntity
                .status(HttpStatus.valueOf(ErrorCode.VALIDATION_FAILED.getStatus()))
                .body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, request.getRequestURI()));
    }

    /**
     * 업로드 파일이 spring.servlet.multipart.max-file-size를 넘으면 컨트롤러 도달 전에 여기서 잡힙니다.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(final MaxUploadSizeExceededException e, final HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.valueOf(ErrorCode.FILE_SIZE_EXCEEDED.getStatus()))
                .body(ErrorResponse.of(ErrorCode.FILE_SIZE_EXCEEDED, request.getRequestURI()));
    }

    /**
     * 그 외 모든 예외 처리. 위에서 잡히지 않은 모든 에러는 여기서 잡아 500으로 응답합니다.
     * [수정][fix/exception-nfr] NFR-001: 내부 예외 메시지/스택트레이스는 서버 로그에만 남기고,
     * 클라이언트에는 절대 노출하지 않음 (원문 메시지 대신 고정된 ErrorCode 메시지만 응답)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(final Exception e, final HttpServletRequest request) {
        log.error("처리되지 않은 예외 발생: path={}", request.getRequestURI(), e);
        return ResponseEntity
                .status(500)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI()));
    }
}
