package com.Nbbang.backend.domain.member.repository;

import com.Nbbang.backend.domain.member.entity.CertificateSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CertificateSessionRepository extends JpaRepository<CertificateSession, Long> {
    Optional<CertificateSession> findByUserId(String userId);
}
