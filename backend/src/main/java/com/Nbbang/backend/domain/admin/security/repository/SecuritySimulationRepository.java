package com.Nbbang.backend.domain.admin.security.repository;

import com.Nbbang.backend.domain.admin.security.entity.SecuritySimulation;
import com.Nbbang.backend.domain.admin.security.entity.SecuritySimulationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SecuritySimulationRepository extends JpaRepository<SecuritySimulation, String> {
    Optional<SecuritySimulation> findByAdministratorIdAndIdempotencyKey(
            String administratorId, String idempotencyKey);

    List<SecuritySimulation> findByStatusIn(Collection<SecuritySimulationStatus> statuses);

    boolean existsByProductIdAndStatusIn(
            Long productId, Collection<SecuritySimulationStatus> statuses);

    Optional<SecuritySimulation> findFirstByProductIdAndStatusInOrderByStartedAtDesc(
            Long productId, Collection<SecuritySimulationStatus> statuses);

    Optional<SecuritySimulation> findFirstByAdministratorIdAndStatusInOrderByStartedAtDesc(
            String administratorId, Collection<SecuritySimulationStatus> statuses);
}
