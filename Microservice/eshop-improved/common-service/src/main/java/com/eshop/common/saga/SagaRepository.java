package com.eshop.common.saga;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Saga State Repository
 */
@Repository
public interface SagaRepository extends JpaRepository<SagaState, Long> {

    /**
     * Find by saga ID
     */
    Optional<SagaState> findBySagaId(String sagaId);

    /**
     * Find by correlation ID and saga type (idempotency)
     */
    Optional<SagaState> findByCorrelationIdAndSagaType(String correlationId, String sagaType);

    /**
     * Find timed out sagas
     */
    @Query("""
        SELECT s FROM SagaState s 
        WHERE s.status IN ('STARTED', 'IN_PROGRESS')
        AND s.timeoutAt < :now
        """)
    List<SagaState> findTimedOutSagas(@Param("now") Instant now, Pageable pageable);

    /**
     * Find sagas needing compensation
     */
    @Query("""
        SELECT s FROM SagaState s 
        WHERE s.status = 'COMPENSATING'
        AND s.compensationAttempts < 3
        """)
    List<SagaState> findSagasNeedingCompensation(Pageable pageable);

    /**
     * Find stuck sagas (locked too long)
     */
    @Query("""
        SELECT s FROM SagaState s 
        WHERE s.status IN ('STARTED', 'IN_PROGRESS', 'COMPENSATING')
        AND s.lockedAt < :threshold
        """)
    List<SagaState> findStuckSagas(@Param("threshold") Instant threshold);

    /**
     * Reset stuck sagas
     */
    @Modifying
    @Query("""
        UPDATE SagaState s 
        SET s.lockedBy = NULL, s.lockedAt = NULL, s.updatedAt = :now
        WHERE s.lockedAt < :threshold
        AND s.status IN ('STARTED', 'IN_PROGRESS')
        """)
    int resetStuckSagas(@Param("threshold") Instant threshold, @Param("now") Instant now);

    /**
     * Find failed sagas for review
     */
    @Query("""
        SELECT s FROM SagaState s 
        WHERE s.status = 'COMPENSATION_FAILED'
        ORDER BY s.updatedAt DESC
        """)
    List<SagaState> findFailedSagas(Pageable pageable);

    /**
     * Count by status (for metrics dashboard)
     */
    @Query("""
        SELECT s.status, COUNT(s) 
        FROM SagaState s 
        GROUP BY s.status
        """)
    List<Object[]> countByStatus();

    /**
     * Find sagas by user
     */
    List<SagaState> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    /**
     * Delete old completed sagas (cleanup)
     */
    @Modifying
    @Query("""
        DELETE FROM SagaState s 
        WHERE s.status IN ('COMPLETED', 'COMPENSATED')
        AND s.completedAt < :threshold
        """)
    int deleteOldCompletedSagas(@Param("threshold") Instant threshold);
}
