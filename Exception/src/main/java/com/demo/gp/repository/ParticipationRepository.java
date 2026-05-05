package com.demo.gp.repository;

import com.demo.gp.entity.Participation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ParticipationRepository extends JpaRepository<Participation, Integer> {
    List<Participation> findByGpId(Integer gpId);
}