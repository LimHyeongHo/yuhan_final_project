package com.Nbbang.backend.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;

// 어디서 예외가 나든 여기서 잡아 ErrorResponse 형식으로 응답함
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 직접 던진 CustomException은 ErrorCode에 정해 둔 상태 코드와 메시지로 응답함
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(final CustomException e, final HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(HttpStatus.valueOf(errorCode.getStatus()))
                .body(ErrorResponse.of(errorCode, request.getRequestURI()));
    }

    // @Valid 입력값 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(final MethodArgumentNotValidException e, final HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.valueOf(ErrorCode.VALIDATION_FAILED.getStatus()))
                .body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, request.getRequestURI()));
    }

    // 업로드 용량 제한을 넘은 파일은 컨트롤러에 오기 전에 여기서 잡힘
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(final MaxUploadSizeExceededException e, final HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.valueOf(ErrorCode.FILE_SIZE_EXCEEDED.getStatus()))
                .body(ErrorResponse.of(ErrorCode.FILE_SIZE_EXCEEDED, request.getRequestURI()));
    }

    /**
     * 컨트롤러나 정적 리소스가 없는 경로는 서버 오류가 아니라 404로 응답합니다.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(final NoResourceFoundException e, final HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND, request.getRequestURI()));
    }

    // 위에서 못 잡은 모든 예외는 500으로 응답함
    // 내부 오류 내용은 서버 로그에만 남기고 사용자에게는 정해진 메시지만 보냄
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(final Exception e, final HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        log.error("처리되지 않은 예외 발생: errorId={}, path={}, type={}",
                errorId, request.getRequestURI(), e.getClass().getSimpleName());
        return ResponseEntity
                .status(500)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI()));
    }
}
