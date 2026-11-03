package com.Nbbang.backend.domain.auth.repository;

import com.Nbbang.backend.domain.auth.entity.DeviceCert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DeviceCertRepository extends JpaRepository<DeviceCert, Long> {
    Optional<DeviceCert> findByUserId(String userId);
    Optional<DeviceCert> findByDeviceId(String deviceId);
    Optional<DeviceCert> findByCiHash(String ciHash);
}
