package com.ducnhu.payment.saga;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderNumber(String orderNumber);

    // NEW: lấy tối đa 100 payment đang "chờ thanh toán" và đã quá hạn
    List<Payment> findTop100ByStatusAndCreatedAtBefore(String status, Instant createdAt);
}

