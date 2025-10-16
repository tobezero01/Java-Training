package com.ducnhu.order.repository;

import com.ducnhu.order.entity.Order;
import com.ducnhu.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    Optional<Order> findByOrderNumberAndCustomerId(String orderNumber, Integer customerId);

    @Query("""
              SELECT (COUNT(od) > 0)
              FROM OrderDetail od JOIN od.order o
              WHERE o.customerId = :customerId AND od.productId = :productId
                AND EXISTS (SELECT 1 FROM OrderTrack ot WHERE ot.order = o AND ot.status IN :statuses)
            """)
    boolean existsPurchased(@Param("customerId") Integer customerId,
                            @Param("productId") Integer productId,
                            @Param("statuses") Collection<OrderStatus> statuses);
}
