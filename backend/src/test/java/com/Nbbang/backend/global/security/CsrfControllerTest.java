package com.Nbbang.backend.global.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.CsrfToken;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CsrfControllerTest {

    @Test
    void returnsTheTokenAndHeaderNameUsedByTheSpa() {
        CsrfToken csrfToken = mock(CsrfToken.class);
        when(csrfToken.getHeaderName()).thenReturn("X-XSRF-TOKEN");
        when(csrfToken.getToken()).thenReturn("test-token");

        Map<String, String> response = new CsrfController().token(csrfToken);

        assertThat(response).containsExactlyInAnyOrderEntriesOf(Map.of(
                "headerName", "X-XSRF-TOKEN",
                "token", "test-token"
        ));
    }
}
