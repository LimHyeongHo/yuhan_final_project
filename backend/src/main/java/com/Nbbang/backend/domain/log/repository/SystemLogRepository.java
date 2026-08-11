package com.Nbbang.backend.domain.log.repository;

import com.Nbbang.backend.domain.log.entity.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
    List<SystemLog> findByTypeOrderByTimestampDesc(String type);
}
