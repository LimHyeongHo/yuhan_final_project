package com.Nbbang.backend.domain.search.service;

import com.Nbbang.backend.domain.search.dto.KakaoBookSnapshot;
import com.Nbbang.backend.global.exception.CustomException;
import com.Nbbang.backend.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
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
            KakaoBookResponse response = requestBooks(new SearchRequest(
                    normalizedIsbn, "isbn", null, null, RESULT_SIZE));
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
        } catch (CustomException e) {
            log.warn("Kakao official price lookup failed: code={}", e.getErrorCode().name());
        }

        return null;
    }

    public List<KakaoBookSnapshot> searchLatestBooks(String query, int page, int size) {
        if (query == null || query.trim().isEmpty()) {
            throw new CustomException(ErrorCode.BOOK_SEARCH_INVALID_QUERY);
        }
        if (page < 1 || page > 50 || size < 1 || size > 50) {
            throw new CustomException(ErrorCode.BOOK_SEARCH_INVALID_QUERY);
        }

        KakaoBookResponse response = requestBooks(new SearchRequest(
                query.trim(), null, "latest", page, size));
        return mapLatestDocuments(response);
    }

    private KakaoBookResponse requestBooks(SearchRequest searchRequest) {
        validateApiKey();

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(BOOK_SEARCH_URL)
                .queryParam("query", searchRequest.query())
                .queryParam("size", searchRequest.size());
        if (searchRequest.target() != null) {
            uriBuilder.queryParam("target", searchRequest.target());
        }
        if (searchRequest.sort() != null) {
            uriBuilder.queryParam("sort", searchRequest.sort());
        }
        if (searchRequest.page() != null) {
            uriBuilder.queryParam("page", searchRequest.page());
        }
        URI uri = uriBuilder.build().encode().toUri();

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
            throw new CustomException(ErrorCode.BOOK_SEARCH_UPSTREAM_ERROR);
        } catch (ResourceAccessException e) {
            ErrorCode errorCode = causedByTimeout(e)
                    ? ErrorCode.BOOK_SEARCH_TIMEOUT
                    : ErrorCode.BOOK_SEARCH_UPSTREAM_ERROR;
            log.warn("Kakao book search connection failed: code={}, type={}",
                    errorCode.name(), e.getClass().getSimpleName());
            throw new CustomException(errorCode);
        } catch (RestClientException e) {
            log.warn("Kakao book search request failed: type={}", e.getClass().getSimpleName());
            throw new CustomException(ErrorCode.BOOK_SEARCH_UPSTREAM_ERROR);
        }
    }

    private void validateApiKey() {
        if (restApiKey.isBlank()) {
            log.error("Kakao book search REST API key is not configured");
            throw new CustomException(ErrorCode.BOOK_SEARCH_NOT_CONFIGURED);
        }
    }

    private SearchRequest createSearchRequest(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new CustomException(ErrorCode.BOOK_SEARCH_INVALID_QUERY);
        }

        String trimmedQuery = query.trim();
        String compactQuery = trimmedQuery.replaceAll("[\\s-]", "");
        if (compactQuery.matches("[0-9]{13}") || compactQuery.matches("[0-9]{10}")) {
            return new SearchRequest(compactQuery, "isbn", null, null, RESULT_SIZE);
        }
        return new SearchRequest(trimmedQuery, "title", null, null, RESULT_SIZE);
    }

    private List<KakaoBookSnapshot> mapLatestDocuments(KakaoBookResponse responseBody) {
        if (responseBody == null || responseBody.documents() == null) {
            return List.of();
        }

        List<KakaoBookSnapshot> results = new ArrayList<>();
        for (KakaoBookDocument document : responseBody.documents()) {
            results.add(new KakaoBookSnapshot(
                    cleanText(document.title()),
                    cleanAuthors(document.authors()),
                    cleanText(document.publisher()),
                    findIsbn(document.isbn(), 13),
                    findIsbn(document.isbn(), 10),
                    cleanText(document.contents()),
                    cleanText(document.url()),
                    parsePublishedDate(document.datetime()),
                    toPrice(document.price()),
                    toPrice(document.salePrice()),
                    cleanText(document.status())
            ));
        }
        return results;
    }

    private List<Map<String, String>> mapDocuments(KakaoBookResponse responseBody) {
        List<Map<String, String>> results = new ArrayList<>();
        if (responseBody == null || responseBody.documents() == null) {
            return results;
        }

        for (KakaoBookDocument document : responseBody.documents()) {
            String isbn = normalizeIsbn(document.isbn());
            Map<String, String> book = new LinkedHashMap<>();
            book.put("title", cleanText(document.title()));
            book.put("category", "");
            book.put("author", joinAuthors(document.authors()));
            book.put("maker", cleanText(document.publisher()));
            book.put("brand", "");
            book.put("image", normalizeImageUrl(document.thumbnail()));
            book.put("description", cleanText(document.contents()));
            book.put("price", document.price() == null ? "" : document.price().toString());
            book.put("isbn", isbn);
            results.add(book);
        }
        return results;
    }

    private String joinAuthors(List<String> authors) {
        return String.join(", ", cleanAuthors(authors));
    }

    private static List<String> cleanAuthors(List<String> authors) {
        if (authors == null) {
            return List.of();
        }

        List<String> authorNames = new ArrayList<>();
        for (String author : authors) {
            String name = cleanText(author);
            if (!name.isBlank()) {
                authorNames.add(name);
            }
        }
        return List.copyOf(authorNames);
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

    private static String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return "";
        }
        String normalized = imageUrl.trim().replaceFirst("^http://", "https://");
        if (normalized.matches("^https://[^/]+\\.kakaocdn\\.net/thumb/.*")) {
            return normalized.replaceFirst(
                    "/thumb/R\\d+x\\d+(?:\\.q\\d+)?/",
                    "/thumb/R500x0.q85/"
            );
        }
        return normalized;
    }

    private CustomException mapClientError(HttpClientErrorException e) {
        int status = e.getStatusCode().value();
        if (status == 401 || status == 403) {
            log.warn("Kakao book search authentication failed: status={}", status);
            return new CustomException(ErrorCode.BOOK_SEARCH_AUTH_ERROR);
        }
        if (status == 429) {
            log.warn("Kakao book search quota exceeded");
            return new CustomException(ErrorCode.BOOK_SEARCH_QUOTA_EXCEEDED);
        }

        log.warn("Kakao book search client error: status={}", status);
        return new CustomException(ErrorCode.BOOK_SEARCH_UPSTREAM_ERROR);
    }

    private static String findIsbn(String rawIsbn, int length) {
        if (rawIsbn == null || rawIsbn.isBlank()) {
            return "";
        }
        String pattern = length == 13 ? "[0-9]{13}" : "[0-9]{9}[0-9Xx]";
        for (String token : rawIsbn.trim().split("\\s+")) {
            String normalized = token.replaceAll("-", "");
            if (normalized.matches(pattern)) {
                return normalized.toUpperCase();
            }
        }
        return "";
    }

    private static LocalDate parsePublishedDate(String datetime) {
        if (datetime == null || datetime.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(datetime).toLocalDate();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static BigDecimal toPrice(Integer price) {
        return price == null || price < 0 ? null : BigDecimal.valueOf(price);
    }

    private boolean causedByTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3_000);
        requestFactory.setReadTimeout(5_000);
        return new RestTemplate(requestFactory);
    }

    private record SearchRequest(String query, String target, String sort, Integer page, int size) {
    }

    private record KakaoBookResponse(List<KakaoBookDocument> documents) {
    }

    private record KakaoBookDocument(
            String title,
            List<String> authors,
            String publisher,
            String isbn,
            Integer price,
            @JsonProperty("sale_price") Integer salePrice,
            String contents,
            String thumbnail,
            String url,
            String datetime,
            String status
    ) {
    }
}
