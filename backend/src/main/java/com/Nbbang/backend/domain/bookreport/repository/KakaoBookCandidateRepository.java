package com.Nbbang.backend.domain.bookreport.repository;

import com.Nbbang.backend.domain.bookreport.entity.KakaoBookCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KakaoBookCandidateRepository extends JpaRepository<KakaoBookCandidate, Long> {
    Optional<KakaoBookCandidate> findByIsbn13(String isbn13);
    Optional<KakaoBookCandidate> findFirstByIsbn10(String isbn10);
    Optional<KakaoBookCandidate> findFirstByTitleAndAuthorsJsonAndPublisher(
            String title, String authorsJson, String publisher);
}
