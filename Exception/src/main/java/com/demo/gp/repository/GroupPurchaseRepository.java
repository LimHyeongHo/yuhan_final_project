package com.demo.gp.repository;

import com.demo.gp.entity.GroupPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupPurchaseRepository extends JpaRepository<GroupPurchase, Integer> {
}