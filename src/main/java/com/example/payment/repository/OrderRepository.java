package com.example.payment.repository;

import com.example.payment.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByDeadlineBefore(LocalDateTime dateTime);
}
