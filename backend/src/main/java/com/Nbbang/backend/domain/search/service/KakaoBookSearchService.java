package com.Nbbang.backend.domain.search.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KakaoBookSearchService {

    private static final Logger log = LoggerFactory.getLogger(KakaoBookSearchService.class);
    private static final String BOOK_SEARCH_URL = "https://dapi.kakao.com/v3/search/book";
    private static final int RESULT_SIZE = 10;

    private final RestTemplate restTemplate;
    private final String restApiKey;

    @Autowired
    public KakaoBookSearchService(@Value("${KAKAO_REST_API_KEY:}") String restApiKey) {
        this(createRestTemplate(), restApiKey);
    }

    KakaoBookSearchService(RestTemplate restTemplate, String restApiKey) {
        this.restTemplate = restTemplate;
        this.restApiKey = restApiKey == null ? "" : restApiKey.trim();
    }

    public List<Map<String, String>> searchBook(String query) {
        SearchRequest searchRequest = createSearchRequest(query);
        return mapDocuments(requestBooks(searchRequest));
    }

    public BigDecimal fetchOfficialPrice(String isbn) {
        String normalizedIsbn = normalizeIsbn(isbn);
        if (normalizedIsbn.isBlank()) {
            return null;
        }

        try {
            KakaoBookResponse response = requestBooks(new SearchRequest(normalizedIsbn, "isbn"));
            if (response == null || response.documents() == null) {
                return null;
            }

            for (KakaoBookDocument document : response.documents()) {
                if (containsIsbn(document.isbn(), normalizedIsbn)
                        && document.price() != null
                        && document.price() > 0) {
                    return BigDecimal.valueOf(document.price());
                }
            }
        } catch (BookSearchException e) {
            log.warn("Kakao official price lookup failed: code={}", e.getCode());
        }

        return null;
    }

    private KakaoBookResponse requestBooks(SearchRequest searchRequest) {
        validateApiKey();

        URI uri = UriComponentsBuilder.fromUriString(BOOK_SEARCH_URL)
                .queryParam("query", searchRequest.query())
                .queryParam("target", searchRequest.target())
                .queryParam("size", RESULT_SIZE)
                .build()
                .encode()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "KakaoAK " + restApiKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<KakaoBookResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    KakaoBookResponse.class
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw mapClientError(e);
        } catch (HttpServerErrorException e) {
            log.warn("Kakao book search server error: status={}", e.getStatusCode().value());
            throw new BookSearchException(
                    HttpStatus.BAD_GATEWAY,
                    "BOOK_SEARCH_UPSTREAM_ERROR",
                    "도서 검색 서비스에 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
            );
        } catch (ResourceAccessException e) {
            log.warn("Kakao book search timed out or could not be reached");
            throw new BookSearchException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "BOOK_SEARCH_TIMEOUT",
                    "도서 검색 서비스 응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요."
            );
        } catch (RestClientException e) {
            log.warn("Kakao book search request failed: type={}", e.getClass().getSimpleName());
            throw new BookSearchException(
                    HttpStatus.BAD_GATEWAY,
                    "BOOK_SEARCH_UPSTREAM_ERROR",
                    "도서 검색 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요."
            );
        }
    }

    private void validateApiKey() {
        if (restApiKey.isBlank()) {
            log.error("Kakao book search REST API key is not configured");
            throw new BookSearchException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "BOOK_SEARCH_NOT_CONFIGURED",
                    "도서 검색 서비스가 설정되지 않았습니다. 관리자에게 문의해 주세요."
            );
        }
    }

    private SearchRequest createSearchRequest(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해 주세요.");
        }

        String trimmedQuery = query.trim();
        String compactQuery = trimmedQuery.replaceAll("[\\s-]", "");
        if (compactQuery.matches("[0-9]{13}") || compactQuery.matches("[0-9]{10}")) {
            return new SearchRequest(compactQuery, "isbn");
        }
        return new SearchRequest(trimmedQuery, "title");
    }

    private List<Map<String, String>> mapDocuments(KakaoBookResponse responseBody) {
        List<Map<String, String>> results = new ArrayList<>();
        if (responseBody == null || responseBody.documents() == null) {
            return results;
        }

        for (KakaoBookDocument document : responseBody.documents()) {
            Map<String, String> book = new LinkedHashMap<>();
            book.put("title", cleanText(document.title()));
            book.put("category", "");
            book.put("author", joinAuthors(document.authors()));
            book.put("maker", cleanText(document.publisher()));
            book.put("brand", "");
            book.put("image", "");
            book.put("description", cleanText(document.contents()));
            book.put("price", document.price() == null ? "" : document.price().toString());
            book.put("isbn", normalizeIsbn(document.isbn()));
            results.add(book);
        }
        return results;
    }

    private String joinAuthors(List<String> authors) {
        if (authors == null) {
            return "";
        }

        List<String> authorNames = new ArrayList<>();
        for (String author : authors) {
            String name = cleanText(author);
            if (!name.isBlank()) {
                authorNames.add(name);
            }
        }
        return String.join(", ", authorNames);
    }

    static String normalizeIsbn(String rawIsbn) {
        if (rawIsbn == null || rawIsbn.isBlank()) {
            return "";
        }

        String isbn10 = "";
        for (String token : rawIsbn.trim().split("\\s+")) {
            String normalized = token.replaceAll("-", "");
            if (normalized.matches("[0-9]{13}")) {
                return normalized;
            }
            if (isbn10.isEmpty() && normalized.matches("[0-9]{9}[0-9Xx]")) {
                isbn10 = normalized.toUpperCase();
            }
        }
        return isbn10;
    }

    private static boolean containsIsbn(String rawIsbn, String expectedIsbn) {
        if (rawIsbn == null || expectedIsbn == null) {
            return false;
        }

        for (String token : rawIsbn.trim().split("\\s+")) {
            if (token.replaceAll("-", "").equalsIgnoreCase(expectedIsbn)) {
                return true;
            }
        }
        return false;
    }

    private static String cleanText(String value) {
        if (value == null) {
            return "";
        }
        String withoutTags = value.replaceAll("<[^>]*>", "");
        return HtmlUtils.htmlUnescape(withoutTags).trim();
    }

    private BookSearchException mapClientError(HttpClientErrorException e) {
        int status = e.getStatusCode().value();
        if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
            log.warn("Kakao book search authentication failed: status={}", status);
            return new BookSearchException(
                    HttpStatus.BAD_GATEWAY,
                    "BOOK_SEARCH_AUTH_ERROR",
                    "도서 검색 서비스 인증에 실패했습니다. 관리자에게 문의해 주세요."
            );
        }
        if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
            log.warn("Kakao book search quota exceeded");
            return new BookSearchException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "BOOK_SEARCH_QUOTA_EXCEEDED",
                    "오늘 사용할 수 있는 도서 검색 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요."
            );
        }

        log.warn("Kakao book search client error: status={}", status);
        return new BookSearchException(
                HttpStatus.BAD_GATEWAY,
                "BOOK_SEARCH_UPSTREAM_ERROR",
                "도서 검색 요청을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요."
        );
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3_000);
        requestFactory.setReadTimeout(5_000);
        return new RestTemplate(requestFactory);
    }

    private record SearchRequest(String query, String target) {
    }

    private record KakaoBookResponse(List<KakaoBookDocument> documents) {
    }

    private record KakaoBookDocument(
            String title,
            List<String> authors,
            String publisher,
            String isbn,
            Integer price,
            String contents
    ) {
    }
}
