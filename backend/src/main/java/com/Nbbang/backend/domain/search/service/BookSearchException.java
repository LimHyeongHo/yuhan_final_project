package com.Nbbang.backend.domain.search.service;

import org.springframework.http.HttpStatus;

public class BookSearchException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BookSearchException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
