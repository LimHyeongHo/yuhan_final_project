package com.Nbbang.backend.domain.bookreport.service;

import com.Nbbang.backend.domain.search.dto.KakaoBookSnapshot;

import java.util.Locale;

public final class BookCandidateKey {

    private BookCandidateKey() {
    }

    public static String of(KakaoBookSnapshot snapshot) {
        if (snapshot.isbn13() != null && !snapshot.isbn13().isBlank()) {
            return "ISBN13:" + snapshot.isbn13();
        }
        if (snapshot.isbn10() != null && !snapshot.isbn10().isBlank()) {
            return "ISBN10:" + snapshot.isbn10();
        }
        return "META:" + normalize(snapshot.title()) + "|"
                + normalize(String.join(" ", snapshot.authors())) + "|" + normalize(snapshot.publisher());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
