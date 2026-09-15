package com.Nbbang.backend.domain.search.controller;

import com.Nbbang.backend.domain.search.service.GoogleBooksService;
import com.Nbbang.backend.domain.search.service.KakaoBookSearchService;
import com.Nbbang.backend.domain.search.service.NaverSearchService;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import com.Nbbang.backend.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class NaverSearchControllerTest {

    private KakaoBookSearchService kakaoBookSearchService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        NaverSearchService naverSearchService = mock(NaverSearchService.class);
        kakaoBookSearchService = mock(KakaoBookSearchService.class);
        GoogleBooksService googleBooksService = mock(GoogleBooksService.class);
        NaverSearchController controller = new NaverSearchController(
                naverSearchService,
                kakaoBookSearchService,
                googleBooksService
        );

        mockMvc = standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsTheGlobalErrorResponseForBookSearchFailures() throws Exception {
        when(kakaoBookSearchService.searchBook("운영체제"))
                .thenThrow(new CustomException(ErrorCode.BOOK_SEARCH_TIMEOUT));

        mockMvc.perform(get("/api/search/product")
                        .param("query", "운영체제")
                        .param("type", "BOOK"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BOOK_SEARCH_TIMEOUT"))
                .andExpect(jsonPath("$.message").value(ErrorCode.BOOK_SEARCH_TIMEOUT.getMessage()))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/search/product"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }
}
