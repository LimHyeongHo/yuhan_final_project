package com.Nbbang.backend.domain.search.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Service
public class GoogleBooksService {

    private static final Logger log = LoggerFactory.getLogger(GoogleBooksService.class);
    private static final String VOLUMES_URL = "https://www.googleapis.com/books/v1/volumes";

    private final RestTemplate restTemplate;
    private final String apiKey;

    @Autowired
    public GoogleBooksService(@Value("${GOOGLE_BOOKS_API_KEY:}") String apiKey) {
        this(createRestTemplate(), apiKey);
    }

    GoogleBooksService(RestTemplate restTemplate, String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public BookMetadata findByIsbn(String isbn) {
        String normalizedIsbn = normalizeIsbn(isbn);
        if (normalizedIsbn.isBlank() || apiKey.isBlank()) {
            return BookMetadata.empty();
        }

        URI uri = UriComponentsBuilder.fromUriString(VOLUMES_URL)
                .queryParam("q", "isbn:" + normalizedIsbn)
                .queryParam("printType", "books")
                .queryParam("maxResults", 5)
                .queryParam("fields", "items(volumeInfo(industryIdentifiers,mainCategory,categories,imageLinks))")
                .queryParam("key", apiKey)
                .build()
                .encode()
                .toUri();

        try {
            ResponseEntity<GoogleBooksResponse> response = restTemplate.getForEntity(uri, GoogleBooksResponse.class);
            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null
                    || response.getBody().items() == null) {
                return BookMetadata.empty();
            }

            List<GoogleVolume> items = response.getBody().items();
            for (GoogleVolume volume : items) {
                GoogleVolumeInfo info = volume.volumeInfo();
                if (info != null && containsIsbn(info.industryIdentifiers(), normalizedIsbn)) {
                    return new BookMetadata(selectImage(info.imageLinks()), selectCategory(info));
                }
            }

            // Google이 ISBN 검색 결과를 한 권만 반환하면서 식별자를 OTHER로 제공하는 경우가 있다.
            if (items.size() == 1 && items.get(0).volumeInfo() != null) {
                GoogleVolumeInfo info = items.get(0).volumeInfo();
                return new BookMetadata(selectImage(info.imageLinks()), selectCategory(info));
            }
        } catch (RestClientException e) {
            log.warn("Google Books metadata lookup failed: type={}", e.getClass().getSimpleName());
        }

        return BookMetadata.empty();
    }

    private static boolean containsIsbn(List<IndustryIdentifier> identifiers, String expectedIsbn) {
        if (identifiers == null) {
            return false;
        }
        return identifiers.stream()
                .map(IndustryIdentifier::identifier)
                .filter(identifier -> identifier != null)
                .map(GoogleBooksService::normalizeIsbn)
                .anyMatch(expectedIsbn::equalsIgnoreCase);
    }

    private static String selectImage(ImageLinks imageLinks) {
        if (imageLinks == null) {
            return "";
        }

        String[] candidates = {
                imageLinks.extraLarge(),
                imageLinks.large(),
                imageLinks.medium(),
                imageLinks.small()
        };
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.replaceFirst("^http://", "https://");
            }
        }

        String thumbnail = imageLinks.thumbnail() != null && !imageLinks.thumbnail().isBlank()
                ? imageLinks.thumbnail()
                : imageLinks.smallThumbnail();
        if (thumbnail == null || thumbnail.isBlank()) {
            return "";
        }

        return thumbnail
                .replaceFirst("^http://", "https://")
                .replaceFirst("([?&])zoom=1(?=(&|$))", "$1zoom=3");
    }

    private static String selectCategory(GoogleVolumeInfo info) {
        if (info.mainCategory() != null && !info.mainCategory().isBlank()) {
            return info.mainCategory().trim();
        }
        if (info.categories() != null) {
            return info.categories().stream()
                    .filter(category -> category != null && !category.isBlank())
                    .map(String::trim)
                    .findFirst()
                    .orElse("");
        }
        return "";
    }

    private static String normalizeIsbn(String isbn) {
        return isbn == null ? "" : isbn.replaceAll("[\\s-]", "").trim();
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3_000);
        requestFactory.setReadTimeout(5_000);
        return new RestTemplate(requestFactory);
    }

    public record BookMetadata(String imageUrl, String category) {
        static BookMetadata empty() {
            return new BookMetadata("", "");
        }
    }

    private record GoogleBooksResponse(List<GoogleVolume> items) {
    }

    private record GoogleVolume(GoogleVolumeInfo volumeInfo) {
    }

    private record GoogleVolumeInfo(
            List<IndustryIdentifier> industryIdentifiers,
            String mainCategory,
            List<String> categories,
            ImageLinks imageLinks
    ) {
    }

    private record IndustryIdentifier(String type, String identifier) {
    }

    private record ImageLinks(
            String smallThumbnail,
            String thumbnail,
            String small,
            String medium,
            String large,
            String extraLarge
    ) {
    }
}
