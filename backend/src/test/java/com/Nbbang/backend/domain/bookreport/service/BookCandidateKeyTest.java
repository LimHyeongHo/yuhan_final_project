package com.Nbbang.backend.domain.bookreport.service;

import com.Nbbang.backend.domain.search.dto.KakaoBookSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BookCandidateKeyTest {

    @Test
    void prefersIsbn13ThenIsbn10() {
        assertThat(BookCandidateKey.of(snapshot("9781234567897", "123456789X", "제목", List.of("저자"))))
                .isEqualTo("ISBN13:9781234567897");
        assertThat(BookCandidateKey.of(snapshot("", "123456789X", "제목", List.of("저자"))))
                .isEqualTo("ISBN10:123456789X");
    }

    @Test
    void fallsBackToNormalizedMetadataWithoutIsbn() {
        KakaoBookSnapshot first = snapshot("", "", " 자바   입문 ", List.of("홍길동"));
        KakaoBookSnapshot second = snapshot("", "", "자바 입문", List.of("홍길동"));

        assertThat(BookCandidateKey.of(first)).isEqualTo(BookCandidateKey.of(second));
    }

    private KakaoBookSnapshot snapshot(String isbn13, String isbn10, String title, List<String> authors) {
        return new KakaoBookSnapshot(
                title, authors, "유한출판", isbn13, isbn10, "", "", null, null, null, "");
    }
}
