package com.Nbbang.backend.domain.bookreport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "kakao_book_candidate", uniqueConstraints = {
        @UniqueConstraint(name = "uk_kakao_book_candidate_isbn13", columnNames = "isbn13")
})
@Getter
@Setter
@NoArgsConstructor
public class KakaoBookCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 13)
    private String isbn13;

    @Column(length = 10)
    private String isbn10;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String authorsJson;

    @Column(length = 300)
    private String publisher;

    @Column(columnDefinition = "TEXT")
    private String contents;

    private LocalDate publishedAt;

    @Column(precision = 12, scale = 0)
    private BigDecimal price;

    @Column(precision = 12, scale = 0)
    private BigDecimal salePrice;

    @Column(length = 100)
    private String saleStatusRaw;

    @Column(length = 1000)
    private String sourceUrl;

    @Column(nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(nullable = false)
    private LocalDateTime sourceFetchedAt;

    @Column(nullable = false, length = 64)
    private String sourceHash;
}
