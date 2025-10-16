package com.ducnhu.cart.repository;

import com.ducnhu.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCustomerIdOrderByIdAsc(Integer customerId);
    Optional<CartItem> findByCustomerIdAndProductId(Integer customerId, Integer productId);
    void deleteByCustomerId(Integer customerId);
    void deleteByCustomerIdAndProductId(Integer customerId, Integer productId);
}
