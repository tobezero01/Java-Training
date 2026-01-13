package com.eshop.common.kafka.consumer;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.function.Supplier;

/**
 * Idempotent Consumer Service
 * 
 * Ensures each message is processed exactly once using a processed_events table.
 * Uses the eventId from Kafka message headers for deduplication.
 */
@Service
@Slf4j
public class IdempotentConsumer {

    private final ProcessedEventRepository repository;

    public IdempotentConsumer(ProcessedEventRepository repository) {
        this.repository = repository;
    }

    /**
     * Process event idempotently
     * 
     * @param eventId Unique event identifier
     * @param eventType Event type/class name
     * @param aggregateType Aggregate type (e.g., "Order")
     * @param aggregateId Aggregate ID (e.g., order number)
     * @param processor Business logic to execute
     * @return true if processed, false if already processed (duplicate)
     */
    @Transactional
    public boolean processIdempotently(
            String eventId,
            String eventType,
            String aggregateType,
            String aggregateId,
            Runnable processor) {
        
        // Check if already processed
        if (repository.existsByEventId(eventId)) {
            log.debug("Event {} already processed, skipping", eventId);
            return false;
        }

        try {
            // Record event as processed FIRST (optimistic approach)
            ProcessedEvent record = ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .processedAt(Instant.now())
                .build();
            
            repository.save(record);
            
            // Execute business logic
            processor.run();
            
            log.debug("Successfully processed event {}", eventId);
            return true;
            
        } catch (Exception e) {
            // If processing fails, we need to remove the record
            // so the message can be retried
            repository.deleteByEventId(eventId);
            throw e;
        }
    }

    /**
     * Process event with return value
     */
    @Transactional
    public <T> IdempotentResult<T> processIdempotently(
            String eventId,
            String eventType,
            String aggregateType,
            String aggregateId,
            Supplier<T> processor) {
        
        if (repository.existsByEventId(eventId)) {
            log.debug("Event {} already processed, skipping", eventId);
            return IdempotentResult.duplicate();
        }

        try {
            ProcessedEvent record = ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .processedAt(Instant.now())
                .build();
            
            repository.save(record);
            
            T result = processor.get();
            
            log.debug("Successfully processed event {}", eventId);
            return IdempotentResult.success(result);
            
        } catch (Exception e) {
            repository.deleteByEventId(eventId);
            throw e;
        }
    }

    /**
     * Check if event was already processed
     */
    public boolean isProcessed(String eventId) {
        return repository.existsByEventId(eventId);
    }

    /**
     * Result wrapper for idempotent processing
     */
    @Data
    @AllArgsConstructor
    public static class IdempotentResult<T> {
        private boolean processed;
        private boolean duplicate;
        private T result;

        public static <T> IdempotentResult<T> success(T result) {
            return new IdempotentResult<>(true, false, result);
        }

        public static <T> IdempotentResult<T> duplicate() {
            return new IdempotentResult<>(false, true, null);
        }
    }
}

/**
 * Processed Event Entity
 */
@Entity
@Table(name = "processed_events", indexes = {
    @Index(name = "idx_processed_events_aggregate", columnList = "aggregate_type, aggregate_id"),
    @Index(name = "idx_processed_events_processed", columnList = "processed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 200)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}

/**
 * Repository for processed events
 */
@Repository
interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
    
    boolean existsByEventId(String eventId);
    
    @Modifying
    @Query("DELETE FROM ProcessedEvent p WHERE p.eventId = :eventId")
    void deleteByEventId(@Param("eventId") String eventId);
    
    @Modifying
    @Query("DELETE FROM ProcessedEvent p WHERE p.processedAt < :threshold")
    int deleteOldRecords(@Param("threshold") Instant threshold);
}
