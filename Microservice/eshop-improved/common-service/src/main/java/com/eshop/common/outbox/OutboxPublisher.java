package com.eshop.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Outbox Publisher - Polls and publishes messages to Kafka
 * 
 * Features:
 * 1. Distributed lock (ShedLock) for multi-instance safety
 * 2. Batch processing with configurable size
 * 3. Async Kafka send for better throughput
 * 4. Stuck message recovery
 * 5. Comprehensive metrics
 * 6. Cleanup of old published messages
 */
@Component
@Slf4j
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String instanceId;

    @Value("${outbox.publisher.batch-size:50}")
    private int batchSize;

    @Value("${outbox.publisher.stuck-threshold-seconds:300}")
    private int stuckThresholdSeconds;

    @Value("${outbox.publisher.cleanup-days:7}")
    private int cleanupDays;

    // Metrics
    private final Counter publishSuccessCounter;
    private final Counter publishFailureCounter;
    private final Counter stuckRecoveredCounter;
    private final Timer publishTimer;
    private final AtomicInteger pendingCount = new AtomicInteger(0);

    public OutboxPublisher(
            OutboxRepository outboxRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.instanceId = getInstanceId();

        // Initialize metrics
        this.publishSuccessCounter = Counter.builder("outbox.publish.success")
            .description("Successfully published outbox messages")
            .register(meterRegistry);

        this.publishFailureCounter = Counter.builder("outbox.publish.failure")
            .description("Failed outbox message publications")
            .register(meterRegistry);

        this.stuckRecoveredCounter = Counter.builder("outbox.stuck.recovered")
            .description("Stuck messages recovered")
            .register(meterRegistry);

        this.publishTimer = Timer.builder("outbox.publish.time")
            .description("Time to publish outbox message")
            .register(meterRegistry);

        // Gauge for pending messages
        Gauge.builder("outbox.pending.count", pendingCount, AtomicInteger::get)
            .description("Current pending outbox messages")
            .register(meterRegistry);
    }

    /**
     * Main publisher job - runs every 500ms
     * Uses ShedLock for distributed locking
     */
    @Scheduled(fixedDelayString = "${outbox.publisher.interval-ms:500}")
    @SchedulerLock(
        name = "outbox_publisher",
        lockAtLeastFor = "100ms",
        lockAtMostFor = "30s"
    )
    @Transactional
    public void publishPendingMessages() {
        Instant now = Instant.now();
        
        // Fetch pending messages with lock
        List<OutboxMessage> messages = outboxRepository.findPendingForProcessing(
            now, 
            PageRequest.of(0, batchSize)
        );

        if (messages.isEmpty()) {
            return;
        }

        log.debug("Processing {} pending outbox messages", messages.size());
        pendingCount.set(messages.size());

        for (OutboxMessage message : messages) {
            processMessage(message);
        }

        pendingCount.set(0);
    }

    /**
     * Process single message
     */
    private void processMessage(OutboxMessage message) {
        Timer.Sample sample = Timer.start();
        
        try {
            // Mark as in progress
            message.markInProgress(instanceId);
            outboxRepository.save(message);

            // Deserialize and send to Kafka
            sendToKafka(message);

        } catch (Exception e) {
            log.error("Failed to process outbox message id={}: {}", message.getId(), e.getMessage());
            handleFailure(message, e);
        } finally {
            sample.stop(publishTimer);
        }
    }

    /**
     * Send message to Kafka
     */
    private void sendToKafka(OutboxMessage message) {
        try {
            // Deserialize payload to original event type
            Class<?> eventClass = Class.forName(message.getEventType());
            Object event = objectMapper.readValue(message.getPayload(), eventClass);

            // Create Kafka record with headers
            ProducerRecord<String, Object> record = new ProducerRecord<>(
                message.getTopic(),
                null, // partition (let Kafka decide based on key)
                message.getMessageKey(),
                event
            );

            // Add headers for tracing and idempotency
            record.headers()
                .add(new RecordHeader("eventId", message.getEventId().getBytes(StandardCharsets.UTF_8)))
                .add(new RecordHeader("eventType", message.getEventType().getBytes(StandardCharsets.UTF_8)))
                .add(new RecordHeader("aggregateType", message.getAggregateType().getBytes(StandardCharsets.UTF_8)))
                .add(new RecordHeader("aggregateId", message.getAggregateId().getBytes(StandardCharsets.UTF_8)))
                .add(new RecordHeader("source", instanceId.getBytes(StandardCharsets.UTF_8)));

            // Send asynchronously with callback
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(record);
            
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    handleFailure(message, ex);
                } else {
                    handleSuccess(message, result);
                }
            });

        } catch (ClassNotFoundException e) {
            log.error("Event class not found: {}", message.getEventType());
            handleFailure(message, e);
        } catch (Exception e) {
            log.error("Failed to deserialize/send message: {}", e.getMessage());
            handleFailure(message, e);
        }
    }

    /**
     * Handle successful publish
     */
    @Transactional
    public void handleSuccess(OutboxMessage message, SendResult<String, Object> result) {
        message.markPublished();
        outboxRepository.save(message);
        publishSuccessCounter.increment();
        
        log.debug("Published outbox message: id={}, eventId={}, topic={}, partition={}, offset={}",
            message.getId(),
            message.getEventId(),
            result.getRecordMetadata().topic(),
            result.getRecordMetadata().partition(),
            result.getRecordMetadata().offset());
    }

    /**
     * Handle publish failure
     */
    @Transactional
    public void handleFailure(OutboxMessage message, Throwable error) {
        String errorMessage = error.getMessage();
        if (errorMessage != null && errorMessage.length() > 2000) {
            errorMessage = errorMessage.substring(0, 2000);
        }
        
        message.markFailed(errorMessage);
        outboxRepository.save(message);
        publishFailureCounter.increment();
        
        if (message.getStatus() == OutboxStatus.DEAD) {
            log.error("Outbox message moved to DEAD: id={}, eventId={}, retries={}",
                message.getId(), message.getEventId(), message.getRetryCount());
        } else {
            log.warn("Outbox message failed, will retry: id={}, eventId={}, attempt={}, nextRetry={}",
                message.getId(), message.getEventId(), message.getRetryCount(), message.getNextRetryAt());
        }
    }

    /**
     * Recovery job - reset stuck messages
     * Runs every minute
     */
    @Scheduled(fixedRateString = "${outbox.publisher.recovery-interval-ms:60000}")
    @SchedulerLock(
        name = "outbox_recovery",
        lockAtLeastFor = "10s",
        lockAtMostFor = "5m"
    )
    @Transactional
    public void recoverStuckMessages() {
        Instant threshold = Instant.now().minusSeconds(stuckThresholdSeconds);
        int recovered = outboxRepository.resetStuckMessages(threshold, Instant.now());
        
        if (recovered > 0) {
            stuckRecoveredCounter.increment(recovered);
            log.warn("Recovered {} stuck outbox messages", recovered);
        }
    }

    /**
     * Cleanup job - delete old published messages
     * Runs daily at 3 AM
     */
    @Scheduled(cron = "${outbox.publisher.cleanup-cron:0 0 3 * * *}")
    @SchedulerLock(
        name = "outbox_cleanup",
        lockAtLeastFor = "1m",
        lockAtMostFor = "30m"
    )
    @Transactional
    public void cleanupOldMessages() {
        Instant threshold = Instant.now().minusSeconds(cleanupDays * 24L * 60 * 60);
        int deleted = outboxRepository.deleteOldPublishedMessages(threshold);
        
        if (deleted > 0) {
            log.info("Cleaned up {} old published outbox messages older than {} days", deleted, cleanupDays);
        }
    }

    /**
     * Get instance identifier for locking
     */
    private static String getInstanceId() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            long pid = ProcessHandle.current().pid();
            return hostname + "-" + pid;
        } catch (Exception e) {
            return "unknown-" + System.currentTimeMillis();
        }
    }
}
