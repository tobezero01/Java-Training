package com.eshop.common.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Outbox Repository with optimized queries for high throughput
 */
@Repository
public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {

    /**
     * Find pending messages ready for processing
     * Uses PESSIMISTIC_WRITE lock to prevent concurrent processing
     * Uses SKIP_LOCKED to avoid blocking on locked rows
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0")) // Skip if locked
    @Query("""
        SELECT m FROM OutboxMessage m 
        WHERE m.status IN ('PENDING', 'FAILED')
        AND (m.nextRetryAt IS NULL OR m.nextRetryAt <= :now)
        ORDER BY m.id ASC
        """)
    List<OutboxMessage> findPendingForProcessing(@Param("now") Instant now, Pageable pageable);

    /**
     * Find messages stuck in IN_PROGRESS state (lock timeout recovery)
     */
    @Query("""
        SELECT m FROM OutboxMessage m
        WHERE m.status = 'IN_PROGRESS'
        AND m.lockedAt < :threshold
        """)
    List<OutboxMessage> findStuckMessages(@Param("threshold") Instant threshold);

    /**
     * Reset stuck messages to PENDING
     */
    @Modifying
    @Query("""
        UPDATE OutboxMessage m
        SET m.status = 'PENDING', 
            m.lockedBy = NULL, 
            m.lockedAt = NULL,
            m.updatedAt = :now
        WHERE m.status = 'IN_PROGRESS'
        AND m.lockedAt < :threshold
        """)
    int resetStuckMessages(@Param("threshold") Instant threshold, @Param("now") Instant now);

    /**
     * Find dead messages for manual review
     */
    @Query("""
        SELECT m FROM OutboxMessage m
        WHERE m.status = 'DEAD'
        ORDER BY m.createdAt DESC
        """)
    List<OutboxMessage> findDeadMessages(Pageable pageable);

    /**
     * Delete old published messages (cleanup job)
     */
    @Modifying
    @Query("""
        DELETE FROM OutboxMessage m
        WHERE m.status = 'PUBLISHED'
        AND m.publishedAt < :threshold
        """)
    int deleteOldPublishedMessages(@Param("threshold") Instant threshold);

    /**
     * Count by status (for metrics)
     */
    @Query("""
        SELECT m.status, COUNT(m) 
        FROM OutboxMessage m 
        GROUP BY m.status
        """)
    List<Object[]> countByStatus();

    /**
     * Find by event ID (for idempotency check)
     */
    Optional<OutboxMessage> findByEventId(String eventId);

    /**
     * Find by aggregate (for saga correlation)
     */
    @Query("""
        SELECT m FROM OutboxMessage m
        WHERE m.aggregateType = :aggregateType
        AND m.aggregateId = :aggregateId
        ORDER BY m.createdAt ASC
        """)
    List<OutboxMessage> findByAggregate(
        @Param("aggregateType") String aggregateType,
        @Param("aggregateId") String aggregateId);

    /**
     * Check if event already exists (idempotency)
     */
    boolean existsByEventId(String eventId);
}
