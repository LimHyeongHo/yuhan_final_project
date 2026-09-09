package com.Nbbang.backend.domain.admin.security.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "security_simulation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_security_simulation_admin_request",
                columnNames = {"administrator_id", "idempotency_key"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecuritySimulation {

    @Id
    @Column(name = "simulation_id", length = 36, updatable = false)
    private String simulationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "simulation_mode", nullable = false, length = 20, updatable = false)
    private SecuritySimulationMode mode;

    @Column(name = "administrator_id", nullable = false, length = 255, updatable = false)
    private String administratorId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    @Column(name = "product_title", nullable = false, updatable = false)
    private String productTitle;

    @Column(name = "original_price", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal originalPrice;

    @Column(name = "tampered_price", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal tamperedPrice;

    @Column(name = "original_db_hash", nullable = false, length = 128, updatable = false)
    private String originalDbHash;

    @Column(name = "active_on_chain_hash", nullable = false, length = 128, updatable = false)
    private String activeOnChainHash;

    @Column(name = "tampered_db_hash", length = 128)
    private String tamperedDbHash;

    @Column(name = "last_observed_on_chain_hash", length = 128)
    private String lastObservedOnChainHash;

    @Column(name = "price_version", nullable = false, updatable = false)
    private Integer priceVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SecuritySimulationStatus status;

    @Column(length = 500, updatable = false)
    private String reason;

    @Column(name = "idempotency_key", length = 100, updatable = false)
    private String idempotencyKey;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "restored_at")
    private LocalDateTime restoredAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(columnDefinition = "TEXT")
    private String message;

    public static SecuritySimulation create(
            SecuritySimulationMode mode,
            String administratorId,
            Long productId,
            String productTitle,
            BigDecimal originalPrice,
            BigDecimal tamperedPrice,
            String originalDbHash,
            String activeOnChainHash,
            Integer priceVersion,
            String reason,
            String idempotencyKey) {
        SecuritySimulation simulation = new SecuritySimulation();
        simulation.simulationId = UUID.randomUUID().toString();
        simulation.mode = mode;
        simulation.administratorId = administratorId;
        simulation.productId = productId;
        simulation.productTitle = productTitle;
        simulation.originalPrice = originalPrice;
        simulation.tamperedPrice = tamperedPrice;
        simulation.originalDbHash = originalDbHash;
        simulation.activeOnChainHash = activeOnChainHash;
        simulation.lastObservedOnChainHash = activeOnChainHash;
        simulation.priceVersion = priceVersion;
        simulation.status = SecuritySimulationStatus.SNAPSHOT_SAVED;
        simulation.reason = reason;
        simulation.idempotencyKey = idempotencyKey;
        simulation.startedAt = LocalDateTime.now();
        simulation.message = "변조 직전 데이터 스냅샷을 저장했습니다.";
        return simulation;
    }

    public void markForgedDetected(String tamperedDbHash) {
        this.tamperedDbHash = tamperedDbHash;
        this.status = SecuritySimulationStatus.FORGED_DETECTED;
        this.message = "DB 데이터와 블록체인 원본 지문이 다릅니다.";
    }

    public void markRestored() {
        this.status = SecuritySimulationStatus.RESTORED;
        this.restoredAt = LocalDateTime.now();
        this.message = "저장된 스냅샷으로 DB 값을 복구했습니다.";
    }

    public void markVerified(String onChainHash) {
        this.lastObservedOnChainHash = onChainHash;
        this.status = SecuritySimulationStatus.VERIFIED;
        this.finishedAt = LocalDateTime.now();
        this.message = "복구된 DB 데이터가 블록체인 원본 지문과 일치합니다.";
    }

    public void markVerificationUnavailable(String message) {
        this.status = SecuritySimulationStatus.VERIFICATION_UNAVAILABLE;
        this.message = message;
    }

    public void markHashMismatch(String onChainHash) {
        this.lastObservedOnChainHash = onChainHash;
        this.status = SecuritySimulationStatus.HASH_MISMATCH;
        this.finishedAt = LocalDateTime.now();
        this.message = "DB 값은 복구했지만 활성 블록체인 지문과 일치하지 않습니다.";
    }

    public void markRecoveryRequired(String message) {
        this.status = SecuritySimulationStatus.RECOVERY_REQUIRED;
        this.finishedAt = LocalDateTime.now();
        this.message = message;
    }
}
