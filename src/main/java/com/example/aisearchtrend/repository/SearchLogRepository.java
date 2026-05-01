package com.example.aisearchtrend.repository;

import com.example.aisearchtrend.dto.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {
    Optional<SearchLog> findByKeyword(String keyword);
    List<SearchLog> findTop10ByOrderBySearchCountDesc();
}
