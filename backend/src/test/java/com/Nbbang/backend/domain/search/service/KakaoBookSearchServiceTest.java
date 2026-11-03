package com.Nbbang.backend.domain.search.service;

import com.Nbbang.backend.domain.search.dto.KakaoBookSnapshot;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoBookSearchServiceTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private KakaoBookSearchService service;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        service = new KakaoBookSearchService(restTemplate, "test-rest-api-key");
    }

    @Test
    void searchesByTitleAndMapsKakaoThumbnailImmediately() {
        server.expect(request -> {
                    Map<String, String> params = UriComponentsBuilder.fromUri(request.getURI())
                            .build()
                            .getQueryParams()
                            .toSingleValueMap();
                    assertThat(UriUtils.decode(params.get("query"), StandardCharsets.UTF_8))
                            .isEqualTo("운영체제");
                    assertThat(params.get("target")).isEqualTo("title");
                    assertThat(params.get("size")).isEqualTo("10");
                })
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "KakaoAK test-rest-api-key"))
                .andRespond(withSuccess("""
                        {
                          "documents": [{
                            "title": "<b>운영체제</b> &amp; 시스템",
                            "authors": ["홍길동", "김유한"],
                            "publisher": "유한출판사",
                            "isbn": "123456789X 9781234567897",
                            "price": 35000,
                            "contents": "전공 도서 설명",
                            "thumbnail": "https://search1.kakaocdn.net/thumb/R120x174.q85/?fname=cover.jpg"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<Map<String, String>> results = service.searchBook("  운영체제  ");

        assertThat(results).hasSize(1);
        assertThat(results.get(0)).containsEntry("title", "운영체제 & 시스템")
                .containsEntry("author", "홍길동, 김유한")
                .containsEntry("maker", "유한출판사")
                .containsEntry("price", "35000")
                .containsEntry("isbn", "9781234567897")
                .containsEntry("image", "https://search1.kakaocdn.net/thumb/R500x0.q85/?fname=cover.jpg");
        server.verify();
    }

    @Test
    void removesHyphensAndSearchesByIsbn() {
        server.expect(request -> {
                    Map<String, String> params = UriComponentsBuilder.fromUri(request.getURI())
                            .build()
                            .getQueryParams()
                            .toSingleValueMap();
                    assertThat(params.get("query")).isEqualTo("9781234567897");
                    assertThat(params.get("target")).isEqualTo("isbn");
                })
                .andRespond(withSuccess("{\"documents\":[]}", MediaType.APPLICATION_JSON));

        assertThat(service.searchBook("978-1-23456-789-7")).isEmpty();
        server.verify();
    }

    @Test
    void fetchesOfficialPriceOnlyFromTheExactIsbnMatch() {
        server.expect(request -> {
                    Map<String, String> params = UriComponentsBuilder.fromUri(request.getURI())
                            .build()
                            .getQueryParams()
                            .toSingleValueMap();
                    assertThat(params.get("query")).isEqualTo("9781234567897");
                    assertThat(params.get("target")).isEqualTo("isbn");
                })
                .andExpect(header("Authorization", "KakaoAK test-rest-api-key"))
                .andRespond(withSuccess("""
                        {
                          "documents": [
                            {"isbn": "1111111111 9781111111111", "price": 99000},
                            {"isbn": "123456789X 9781234567897", "price": 35000}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThat(service.fetchOfficialPrice("978-1-23456-789-7"))
                .isEqualByComparingTo("35000");
        server.verify();
    }

    @Test
    void officialPriceLookupReturnsNullWhenKakaoIsUnavailable() {
        server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/v3/search/book"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThat(service.fetchOfficialPrice("9781234567897")).isNull();
        server.verify();
    }

    @Test
    void returnsEmptyListWhenNoDocumentsExist() {
        server.expect(request -> assertThat(request.getURI().getPath())
                        .isEqualTo("/v3/search/book"))
                .andRespond(withSuccess("{\"documents\":[]}", MediaType.APPLICATION_JSON));

        assertThat(service.searchBook("존재하지 않는 책")).isEmpty();
        server.verify();
    }

    @Test
    void rejectsBlankQueryWithoutCallingKakao() {
        assertThatThrownBy(() -> service.searchBook("   "))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOOK_SEARCH_INVALID_QUERY));
        server.verify();
    }

    @Test
    void reportsMissingApiKeyWithoutExposingASecret() {
        KakaoBookSearchService unconfiguredService = new KakaoBookSearchService(restTemplate, "");

        assertThatThrownBy(() -> unconfiguredService.searchBook("운영체제"))
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOOK_SEARCH_NOT_CONFIGURED);
                    assertThat(exception.getMessage()).doesNotContain("KakaoAK");
                });
        server.verify();
    }

    @Test
    void distinguishesAuthenticationAndQuotaErrors() {
        server.expect(request -> assertThat(request.getURI().getHost()).isEqualTo("dapi.kakao.com"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> service.searchBook("운영체제"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOOK_SEARCH_AUTH_ERROR));
        server.verify();

        setUp();
        server.expect(request -> assertThat(request.getURI().getHost()).isEqualTo("dapi.kakao.com"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> service.searchBook("운영체제"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOOK_SEARCH_QUOTA_EXCEEDED));
        server.verify();
    }

    @Test
    void searchesLatestBooksWithReportFields() {
        server.expect(request -> {
                    Map<String, String> params = UriComponentsBuilder.fromUri(request.getURI())
                            .build()
                            .getQueryParams()
                            .toSingleValueMap();
                    assertThat(UriUtils.decode(params.get("query"), StandardCharsets.UTF_8))
                            .isEqualTo("프로그래밍");
                    assertThat(params).doesNotContainKey("target");
                    assertThat(params.get("sort")).isEqualTo("latest");
                    assertThat(params.get("page")).isEqualTo("1");
                    assertThat(params.get("size")).isEqualTo("50");
                })
                .andRespond(withSuccess("""
                        {
                          "documents": [{
                            "title": "자바 프로그래밍",
                            "authors": ["홍길동"],
                            "publisher": "유한출판",
                            "isbn": "123456789X 9781234567897",
                            "price": 30000,
                            "sale_price": 27000,
                            "contents": "프로그래밍 입문서",
                            "url": "https://search.daum.net/search?w=bookpage&bookId=1",
                            "datetime": "2026-10-20T00:00:00.000+09:00",
                            "status": "정상판매"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<KakaoBookSnapshot> results = service.searchLatestBooks(" 프로그래밍 ", 1, 50);

        assertThat(results).singleElement().satisfies(book -> {
            assertThat(book.title()).isEqualTo("자바 프로그래밍");
            assertThat(book.isbn13()).isEqualTo("9781234567897");
            assertThat(book.isbn10()).isEqualTo("123456789X");
            assertThat(book.publishedAt()).isEqualTo("2026-10-20");
            assertThat(book.price()).isEqualByComparingTo("30000");
            assertThat(book.salePrice()).isEqualByComparingTo("27000");
        });
        server.verify();
    }

    @Test
    void mapsMissingLatestBookFieldsWithoutFailing() {
        server.expect(request -> assertThat(request.getURI().getQuery()).contains("sort=latest"))
                .andRespond(withSuccess("""
                        {"documents":[{"title":"제목만 있는 책","authors":null}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(service.searchLatestBooks("테스트", 1, 50)).singleElement().satisfies(book -> {
            assertThat(book.title()).isEqualTo("제목만 있는 책");
            assertThat(book.authors()).isEmpty();
            assertThat(book.publishedAt()).isNull();
            assertThat(book.price()).isNull();
            assertThat(book.salePrice()).isNull();
        });
        server.verify();
    }

    @Test
    void mapsKakaoServerErrorsToTheFixedUpstreamError() {
        server.expect(request -> assertThat(request.getURI().getHost()).isEqualTo("dapi.kakao.com"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> service.searchBook("운영체제"))
                .isInstanceOfSatisfying(CustomException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOOK_SEARCH_UPSTREAM_ERROR));
        server.verify();
    }

    @Test
    void mapsSocketTimeoutWithoutExposingTheExternalMessage() {
        server.expect(request -> assertThat(request.getURI().getHost()).isEqualTo("dapi.kakao.com"))
                .andRespond(request -> {
                    throw new ResourceAccessException(
                            "Authorization KakaoAK secret-key",
                            new SocketTimeoutException("provider timeout detail"));
                });

        assertThatThrownBy(() -> service.searchBook("운영체제"))
                .isInstanceOfSatisfying(CustomException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOOK_SEARCH_TIMEOUT);
                    assertThat(exception.getMessage()).doesNotContain("secret-key", "provider timeout detail");
                });
        server.verify();
    }

    @Test
    void prefersIsbn13AndFallsBackToIsbn10() {
        assertThat(KakaoBookSearchService.normalizeIsbn("123456789X 9781234567897"))
                .isEqualTo("9781234567897");
        assertThat(KakaoBookSearchService.normalizeIsbn("123456789x"))
                .isEqualTo("123456789X");
        assertThat(KakaoBookSearchService.normalizeIsbn("invalid"))
                .isEmpty();
    }
}
