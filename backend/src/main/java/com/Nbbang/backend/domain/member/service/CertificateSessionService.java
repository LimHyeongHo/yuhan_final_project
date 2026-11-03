package com.Nbbang.backend.domain.member.service;

import com.Nbbang.backend.domain.auth.entity.DeviceCert;
import com.Nbbang.backend.domain.auth.repository.DeviceCertRepository;
import com.Nbbang.backend.domain.auth.service.CAService;
import com.Nbbang.backend.domain.member.entity.CertificateSession;
import com.Nbbang.backend.domain.member.repository.CertificateSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CertificateSessionService {

    private static final long DEFAULT_VALID_MINUTES = 10;
    private static final long MAX_VALID_MINUTES = 60; // +버튼으로 늘릴 수 있는 상한 (현재 시각 기준 최대 60분)

    private final CertificateSessionRepository certificateSessionRepository;
    private final DeviceCertRepository deviceCertRepository;
    private final CAService caService;

    public CertificateSessionService(CertificateSessionRepository certificateSessionRepository,
                                      DeviceCertRepository deviceCertRepository,
                                      CAService caService) {
        this.certificateSessionRepository = certificateSessionRepository;
        this.deviceCertRepository = deviceCertRepository;
        this.caService = caService;
    }

    // 로그인 성공 시 호출: 인증서 유효 타이머를 10분으로 (재)시작
    @Transactional
    public CertificateSession startSession(String userId) {
        DeviceCert cert = deviceCertRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("발급된 인증서가 없습니다."));

        CertificateSession session = certificateSessionRepository.findByUserId(userId)
                .orElse(new CertificateSession());

        LocalDateTime now = LocalDateTime.now();
        session.setUserId(userId);
        session.setCertificateSerialNumber(cert.getCertificateSerialNumber());
        session.setIssuedAt(now);
        session.setExpiresAt(now.plusMinutes(DEFAULT_VALID_MINUTES));

        return certificateSessionRepository.save(session);
    }

    public Optional<CertificateSession> findByUserId(String userId) {
        return certificateSessionRepository.findByUserId(userId);
    }

    // 만료된 세션이 "존재"하는 경우에만 true (세션 자체가 없으면 false)
    public boolean hasExpiredSession(String userId) {
        return certificateSessionRepository.findByUserId(userId)
                .map(session -> !session.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    // +5분 / -5분 조정. 서버 DB의 만료시각을 직접 갱신한다.
    @Transactional
    public CertificateSession extend(String userId, int deltaMinutes) {
        CertificateSession session = certificateSessionRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("진행중인 인증서 세션이 없습니다."));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newExpiresAt = session.getExpiresAt().plusMinutes(deltaMinutes);
        if (newExpiresAt.isBefore(now)) {
            newExpiresAt = now; // 현재 시각보다 과거로는 내려가지 않도록 하한 처리
        }
        LocalDateTime maxExpiresAt = now.plusMinutes(MAX_VALID_MINUTES);
        if (newExpiresAt.isAfter(maxExpiresAt)) {
            newExpiresAt = maxExpiresAt; // 현재 시각 기준 60분을 넘지 않도록 상한 처리
        }

        session.setExpiresAt(newExpiresAt);
        return certificateSessionRepository.save(session);
    }

    // 인증서 폐기: CA 서비스 CRL 등록 + DeviceCert.revoked 갱신 + 세션 삭제.
    // N분 타이머 만료 또는 회원 탈퇴처럼 기기 인증서 자체를 못 쓰게 만들어야 하는 경우에 사용.
    // (수동 로그아웃 버튼에서는 호출하지 않음 - Header.jsx는 로컬 세션 정리만 함)
    @Transactional
    public void revoke(String userId) {
        deviceCertRepository.findByUserId(userId).ifPresent(cert -> {
            if (cert.getCertificateSerialNumber() != null) {
                caService.revokeCertificate(new BigInteger(cert.getCertificateSerialNumber()));
            }
            cert.setRevoked(true);
            deviceCertRepository.save(cert);
        });

        certificateSessionRepository.findByUserId(userId)
                .ifPresent(certificateSessionRepository::delete);
    }
}
