package com.springboot.pki.repository;

import com.springboot.pki.entity.DeviceCert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DeviceCertRepository extends JpaRepository<DeviceCert, Long> {
    Optional<DeviceCert> findByUserId(String userId);
    Optional<DeviceCert> findByDeviceId(String deviceId);
    Optional<DeviceCert> findByCiHash(String ciHash);
}
