package com.eshop.common.saga;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Saga State Entity - Tracks the state of a distributed saga
 * 
 * Supports both orchestration and choreography patterns
 */
@Entity
@Table(name = "saga_states", indexes = {
    @Index(name = "idx_saga_status", columnList = "status"),
    @Index(name = "idx_saga_type_correlation", columnList = "saga_type, correlation_id"),
    @Index(name = "idx_saga_timeout", columnList = "status, timeout_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique saga instance ID
     */
    @Column(name = "saga_id", nullable = false, length = 36, unique = true)
    private String sagaId;

    /**
     * Type of saga (e.g., "CHECKOUT", "ORDER_CANCEL")
     */
    @Column(name = "saga_type", nullable = false, length = 50)
    private String sagaType;

    /**
     * Business correlation ID (e.g., order number)
     */
    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    /**
     * Current step in the saga
     */
    @Column(name = "current_step", nullable = false, length = 50)
    private String currentStep;

    /**
     * Current status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SagaStatus status = SagaStatus.STARTED;

    /**
     * Saga payload/context (JSON)
     */
    @Lob
    @Column(name = "payload", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String payload;

    /**
     * Completed steps (JSON array)
     */
    @Column(name = "completed_steps", length = 1000)
    private String completedSteps;

    /**
     * Failed step (if any)
     */
    @Column(name = "failed_step", length = 50)
    private String failedStep;

    /**
     * Failure reason
     */
    @Column(name = "failure_reason", length = 2000)
    private String failureReason;

    /**
     * Number of compensation attempts
     */
    @Column(name = "compensation_attempts")
    @Builder.Default
    private int compensationAttempts = 0;

    /**
     * Timeout timestamp
     */
    @Column(name = "timeout_at")
    private Instant timeoutAt;

    /**
     * Step timeout (per step)
     */
    @Column(name = "step_started_at")
    private Instant stepStartedAt;

    /**
     * Instance handling this saga (for locking)
     */
    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    @Column(name = "locked_at")
    private Instant lockedAt;

    /**
     * User/customer ID for this saga
     */
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.sagaId == null) {
            this.sagaId = UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Create a new saga
     */
    public static SagaState create(
            String sagaType,
            String correlationId,
            String initialStep,
            String payload,
            Integer userId,
            Instant timeout) {
        
        return SagaState.builder()
            .sagaId(UUID.randomUUID().toString())
            .sagaType(sagaType)
            .correlationId(correlationId)
            .currentStep(initialStep)
            .status(SagaStatus.STARTED)
            .payload(payload)
            .completedSteps("[]")
            .userId(userId)
            .timeoutAt(timeout)
            .stepStartedAt(Instant.now())
            .build();
    }

    /**
     * Move to next step
     */
    public void moveToStep(String nextStep, String updatedPayload) {
        // Add current step to completed
        if (this.completedSteps == null || this.completedSteps.equals("[]")) {
            this.completedSteps = "[\"" + this.currentStep + "\"]";
        } else {
            this.completedSteps = this.completedSteps.substring(0, this.completedSteps.length() - 1) 
                + ",\"" + this.currentStep + "\"]";
        }
        
        this.currentStep = nextStep;
        this.stepStartedAt = Instant.now();
        this.status = SagaStatus.IN_PROGRESS;
        
        if (updatedPayload != null) {
            this.payload = updatedPayload;
        }
    }

    /**
     * Mark saga as completed
     */
    public void complete() {
        this.status = SagaStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.lockedBy = null;
        this.lockedAt = null;
    }

    /**
     * Mark saga as failed and start compensation
     */
    public void fail(String step, String reason) {
        this.status = SagaStatus.COMPENSATING;
        this.failedStep = step;
        this.failureReason = reason != null && reason.length() > 2000 
            ? reason.substring(0, 2000) 
            : reason;
    }

    /**
     * Mark compensation as complete
     */
    public void compensated() {
        this.status = SagaStatus.COMPENSATED;
        this.completedAt = Instant.now();
        this.lockedBy = null;
        this.lockedAt = null;
    }

    /**
     * Mark compensation as failed
     */
    public void compensationFailed(String reason) {
        this.compensationAttempts++;
        if (this.compensationAttempts >= 3) {
            this.status = SagaStatus.COMPENSATION_FAILED;
        }
        this.failureReason = reason;
    }

    /**
     * Check if saga has timed out
     */
    public boolean isTimedOut() {
        return this.timeoutAt != null && Instant.now().isAfter(this.timeoutAt);
    }

    /**
     * Acquire lock
     */
    public void lock(String instanceId) {
        this.lockedBy = instanceId;
        this.lockedAt = Instant.now();
    }

    /**
     * Release lock
     */
    public void unlock() {
        this.lockedBy = null;
        this.lockedAt = null;
    }

    /**
     * Check if lock is stale
     */
    public boolean isLockStale(long maxLockSeconds) {
        return this.lockedAt != null && 
               this.lockedAt.plusSeconds(maxLockSeconds).isBefore(Instant.now());
    }
}

/**
 * Saga status enum
 */
enum SagaStatus {
    STARTED,            // Saga just started
    IN_PROGRESS,        // Saga is executing steps
    COMPLETED,          // All steps completed successfully
    COMPENSATING,       // Compensation in progress
    COMPENSATED,        // Compensation completed
    COMPENSATION_FAILED,// Compensation failed - needs manual intervention
    TIMED_OUT           // Saga timed out
}
