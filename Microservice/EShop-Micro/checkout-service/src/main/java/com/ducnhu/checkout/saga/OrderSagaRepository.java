package com.ducnhu.checkout.saga;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderSagaRepository extends JpaRepository<OrderSagaEntity, Long> {
    Optional<OrderSagaEntity> findByOrderNumber(String orderNumber);
}
