package com.Nbbang.backend.domain.admin.security.entity;

public enum SecuritySimulationStatus {
    SNAPSHOT_SAVED,
    FORGED_DETECTED,
    RESTORED,
    VERIFIED,
    VERIFICATION_UNAVAILABLE,
    HASH_MISMATCH,
    RECOVERY_REQUIRED
}
