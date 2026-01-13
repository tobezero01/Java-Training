package com.eshop.common.outbox;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox Message Entity
 * 
 * Stores events to be published to Kafka.
 * Ensures atomic write of business data + event in same transaction.
 * 
 * Status flow: PENDING -> IN_PROGRESS -> PUBLISHED
 *                     |-> FAILED (can retry)
 *                     |-> DEAD (max retries exceeded)
 */
@Entity
@Table(name = "outbox_messages", indexes = {
    @Index(name = "idx_outbox_status_next", columnList = "status, next_retry_at"),
    @Index(name = "idx_outbox_created", columnList = "created_at"),
    @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type, aggregate_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique event ID for idempotency
     */
    @Column(name = "event_id", nullable = false, length = 36, unique = true)
    private String eventId;

    /**
     * Kafka topic to publish to
     */
    @Column(name = "topic", nullable = false, length = 200)
    private String topic;

    /**
     * Kafka message key (for partitioning)
     */
    @Column(name = "message_key", length = 200)
    private String messageKey;

    /**
     * Full class name of the event (for deserialization)
     */
    @Column(name = "event_type", nullable = false, length = 500)
    private String eventType;

    /**
     * Aggregate type (e.g., "Order", "Payment")
     */
    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    /**
     * Aggregate ID (e.g., order number)
     */
    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    /**
     * JSON payload of the event
     */
    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String payload;

    /**
     * Current status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OutboxStatus status = OutboxStatus.PENDING;

    /**
     * Number of retry attempts
     */
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    /**
     * Maximum retries before moving to DEAD
     */
    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private int maxRetries = 5;

    /**
     * Next retry timestamp
     */
    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    /**
     * Instance that locked this message for processing
     */
    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    /**
     * When the lock was acquired
     */
    @Column(name = "locked_at")
    private Instant lockedAt;

    /**
     * Last error message
     */
    @Column(name = "last_error", length = 2000)
    private String lastError;

    /**
     * When the message was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * When the message was last updated
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * When the message was published (if successful)
     */
    @Column(name = "published_at")
    private Instant publishedAt;

    /**
     * Headers to include in Kafka message (JSON)
     */
    @Column(name = "headers", length = 1000)
    private String headers;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.eventId == null) {
            this.eventId = UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Create new outbox message
     */
    public static OutboxMessage create(
            String topic,
            String messageKey,
            String eventType,
            String aggregateType,
            String aggregateId,
            String payload) {
        
        return OutboxMessage.builder()
            .eventId(UUID.randomUUID().toString())
            .topic(topic)
            .messageKey(messageKey)
            .eventType(eventType)
            .aggregateType(aggregateType)
            .aggregateId(aggregateId)
            .payload(payload)
            .status(OutboxStatus.PENDING)
            .retryCount(0)
            .maxRetries(5)
            .build();
    }

    /**
     * Mark as in progress (being processed)
     */
    public void markInProgress(String instanceId) {
        this.status = OutboxStatus.IN_PROGRESS;
        this.lockedBy = instanceId;
        this.lockedAt = Instant.now();
    }

    /**
     * Mark as published successfully
     */
    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        this.lockedBy = null;
        this.lockedAt = null;
        this.lastError = null;
    }

    /**
     * Mark as failed with error
     */
    public void markFailed(String error) {
        this.retryCount++;
        this.lastError = error != null && error.length() > 2000 
            ? error.substring(0, 2000) 
            : error;
        this.lockedBy = null;
        this.lockedAt = null;
        
        if (this.retryCount >= this.maxRetries) {
            this.status = OutboxStatus.DEAD;
        } else {
            this.status = OutboxStatus.FAILED;
            // Exponential backoff: 2^retryCount seconds, max 5 minutes
            long delaySeconds = Math.min(300, (long) Math.pow(2, this.retryCount));
            this.nextRetryAt = Instant.now().plusSeconds(delaySeconds);
        }
    }

    /**
     * Check if message is stuck (locked too long)
     */
    public boolean isStuck(long maxLockDurationSeconds) {
        if (this.lockedAt == null) return false;
        return this.lockedAt.plusSeconds(maxLockDurationSeconds).isBefore(Instant.now());
    }
}

/**
 * Outbox message status
 */
enum OutboxStatus {
    PENDING,      // Waiting to be processed
    IN_PROGRESS,  // Currently being processed
    PUBLISHED,    // Successfully published to Kafka
    FAILED,       // Failed, will retry
    DEAD          // Max retries exceeded, manual intervention needed
}
