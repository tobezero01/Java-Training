package com.ducnhu.payment.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
         SELECT m FROM OutboxMessage m
         WHERE (m.status = 'PENDING' OR m.status = 'FAILED')
           AND (m.nextAttemptAt IS NULL OR m.nextAttemptAt <= :now)
         ORDER BY m.id ASC
         """)
    List<OutboxMessage> pickPendingForUpdate(@Param("now") Instant now, Pageable pageable);
}
