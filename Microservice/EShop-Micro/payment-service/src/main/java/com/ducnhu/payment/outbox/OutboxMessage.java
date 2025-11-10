package com.ducnhu.payment.outbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

// checkout-service/src/main/java/.../outbox/OutboxMessage.java
@Entity
@Table(name = "outbox_messages",
        indexes = @Index(name = "idx_outbox_status_next", columnList = "status,next_attempt_at"))
@Getter
@Setter
public class OutboxMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic", length = 200, nullable = false)
    private String topic;                       // Kafka topic cần publish

    @Column(name = "type", length = 200, nullable = false)
    private String type;                        // FQN class của event (để deserialize gửi Kafka)

    @Column(name = "msg_key", length = 200)
    private String key;                         // Kafka key (ví dụ: orderNumber)

    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;                  // JSON của event

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "locked_by", length = 64)
    private String lockedBy;                    // để lock soft khi multi instance

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    // getters/setters...
}
