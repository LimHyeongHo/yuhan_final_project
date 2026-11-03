package com.Nbbang.backend.domain.search.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleBooksServiceTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private GoogleBooksService service;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        service = new GoogleBooksService(restTemplate, "test-google-books-key");
    }

    @Test
    void returnsHighResolutionImageAndCategoryForExactIsbn() {
        server.expect(request -> {
                    Map<String, String> params = UriComponentsBuilder.fromUri(request.getURI())
                            .build()
                            .getQueryParams()
                            .toSingleValueMap();
                    assertThat(UriUtils.decode(params.get("q"), StandardCharsets.UTF_8))
                            .isEqualTo("isbn:9781234567897");
                    assertThat(params.get("key")).isEqualTo("test-google-books-key");
                    assertThat(params).doesNotContainKey("projection");
                    assertThat(params.get("fields")).contains("imageLinks");
                })
                .andRespond(withSuccess("""
                        {
                          "items": [
                            {
                              "volumeInfo": {
                                "industryIdentifiers": [{"type": "ISBN_13", "identifier": "9781111111111"}],
                                "mainCategory": "Wrong category",
                                "imageLinks": {"extraLarge": "https://example.com/wrong.jpg"}
                              }
                            },
                            {
                              "volumeInfo": {
                                "industryIdentifiers": [{"type": "ISBN_13", "identifier": "9781234567897"}],
                                "mainCategory": "Computers",
                                "categories": ["Computers / Programming"],
                                "imageLinks": {
                                  "thumbnail": "https://example.com/thumb.jpg",
                                  "large": "http://books.google.com/large-cover.jpg"
                                }
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        GoogleBooksService.BookMetadata metadata = service.findByIsbn("978-1-23456-789-7");

        assertThat(metadata.imageUrl()).isEqualTo("https://books.google.com/large-cover.jpg");
        assertThat(metadata.category()).isEqualTo("Computers");
        server.verify();
    }

    @Test
    void missingApiKeyReturnsEmptyMetadataWithoutCallingGoogle() {
        GoogleBooksService unconfiguredService = new GoogleBooksService(restTemplate, "");

        GoogleBooksService.BookMetadata metadata = unconfiguredService.findByIsbn("9781234567897");

        assertThat(metadata.imageUrl()).isEmpty();
        assertThat(metadata.category()).isEmpty();
        server.verify();
    }

    @Test
    void usesSingleIsbnSearchResultWhenGoogleOmitsStandardIsbnIdentifier() {
        server.expect(request -> assertThat(request.getURI().getQuery()).contains("q=isbn"))
                .andRespond(withSuccess("""
                        {
                          "items": [
                            {
                              "volumeInfo": {
                                "industryIdentifiers": [{"type": "OTHER", "identifier": "library-id"}],
                                "categories": ["Computers"],
                                "imageLinks": {"thumbnail": "http://books.google.com/cover.jpg?zoom=1&source=gbs_api"}
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        GoogleBooksService.BookMetadata metadata = service.findByIsbn("9781234567897");

        assertThat(metadata.imageUrl()).isEqualTo("https://books.google.com/cover.jpg?zoom=3&source=gbs_api");
        assertThat(metadata.category()).isEqualTo("Computers");
        server.verify();
    }
}
