package com.eshop.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbox Service for enqueueing events
 * 
 * Usage:
 * 1. Call enqueue() within your business transaction
 * 2. OutboxPublisher will poll and publish to Kafka
 * 
 * This ensures atomic write of business data + event
 */
@Service
@Slf4j
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    
    // Metrics
    private final Counter enqueueCounter;
    private final Counter enqueueErrorCounter;
    private final Timer enqueueTimer;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    public OutboxService(
            OutboxRepository outboxRepository,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        
        // Initialize metrics
        this.enqueueCounter = Counter.builder("outbox.enqueue.total")
            .description("Total outbox messages enqueued")
            .register(meterRegistry);
        
        this.enqueueErrorCounter = Counter.builder("outbox.enqueue.errors")
            .description("Outbox enqueue errors")
            .register(meterRegistry);
        
        this.enqueueTimer = Timer.builder("outbox.enqueue.time")
            .description("Time to enqueue outbox message")
            .register(meterRegistry);
    }

    /**
     * Enqueue event to outbox
     * Must be called within a transaction with business logic
     * 
     * @param topic Kafka topic
     * @param aggregateType Type of aggregate (e.g., "Order")
     * @param aggregateId ID of aggregate (e.g., order number)
     * @param event Event object to publish
     * @return Created OutboxMessage
     */
    @Transactional(propagation = Propagation.MANDATORY) // Must be in existing transaction
    public OutboxMessage enqueue(String topic, String aggregateType, String aggregateId, Object event) {
        return enqueueTimer.record(() -> {
            try {
                String payload = objectMapper.writeValueAsString(event);
                String eventType = event.getClass().getName();
                
                OutboxMessage message = OutboxMessage.builder()
                    .eventId(UUID.randomUUID().toString())
                    .topic(topic)
                    .messageKey(aggregateId) // Use aggregate ID as Kafka key for ordering
                    .eventType(eventType)
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .payload(payload)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .maxRetries(5)
                    .build();
                
                OutboxMessage saved = outboxRepository.save(message);
                
                enqueueCounter.increment();
                log.debug("Enqueued outbox message: topic={}, aggregateType={}, aggregateId={}, eventId={}",
                    topic, aggregateType, aggregateId, saved.getEventId());
                
                return saved;
                
            } catch (JsonProcessingException e) {
                enqueueErrorCounter.increment();
                log.error("Failed to serialize event for outbox: {}", e.getMessage());
                throw new OutboxException("Failed to serialize event", e);
            }
        });
    }

    /**
     * Enqueue with custom message key
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxMessage enqueue(
            String topic, 
            String messageKey,
            String aggregateType, 
            String aggregateId, 
            Object event) {
        
        try {
            String payload = objectMapper.writeValueAsString(event);
            String eventType = event.getClass().getName();
            
            OutboxMessage message = OutboxMessage.builder()
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
            
            OutboxMessage saved = outboxRepository.save(message);
            enqueueCounter.increment();
            
            return saved;
            
        } catch (JsonProcessingException e) {
            enqueueErrorCounter.increment();
            throw new OutboxException("Failed to serialize event", e);
        }
    }

    /**
     * Enqueue with idempotency check
     * If event with same ID already exists, skip
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<OutboxMessage> enqueueIdempotent(
            String eventId,
            String topic,
            String aggregateType,
            String aggregateId,
            Object event) {
        
        // Check if already exists
        if (outboxRepository.existsByEventId(eventId)) {
            log.debug("Event {} already exists in outbox, skipping", eventId);
            return Optional.empty();
        }
        
        try {
            String payload = objectMapper.writeValueAsString(event);
            String eventType = event.getClass().getName();
            
            OutboxMessage message = OutboxMessage.builder()
                .eventId(eventId) // Use provided event ID
                .topic(topic)
                .messageKey(aggregateId)
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .build();
            
            return Optional.of(outboxRepository.save(message));
            
        } catch (JsonProcessingException e) {
            throw new OutboxException("Failed to serialize event", e);
        }
    }

    /**
     * Custom exception for outbox errors
     */
    public static class OutboxException extends RuntimeException {
        public OutboxException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
