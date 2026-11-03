package com.Nbbang.backend.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    @Test
    void returnsNotFoundForMissingResource() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/");

        ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
                .handleNoResourceFoundException(mock(NoResourceFoundException.class), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().getPath()).isEqualTo("/");
    }
}
