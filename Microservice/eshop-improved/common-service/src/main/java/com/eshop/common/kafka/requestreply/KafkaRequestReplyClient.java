package com.eshop.common.kafka.requestreply;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Production-grade Request-Reply Client for Kafka
 * 
 * Features:
 * 1. Circuit Breaker pattern (Resilience4j)
 * 2. Configurable timeout per request
 * 3. Correlation ID management
 * 4. Metrics support
 * 5. Graceful cleanup of expired requests
 */
@Component
@Slf4j
public class KafkaRequestReplyClient {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    // Pending requests map: correlationId -> CompletableFuture
    private final Map<String, CompletableFuture<Object>> pendingRequests = new ConcurrentHashMap<>();
    
    // Scheduled cleanup of expired requests
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor(
        r -> {
            Thread t = new Thread(r, "kafka-reply-cleanup");
            t.setDaemon(true);
            return t;
        }
    );

    public KafkaRequestReplyClient(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        
        // Configure Circuit Breaker
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50) // Open circuit if 50% requests fail
            .slowCallRateThreshold(80) // Open if 80% calls are slow
            .slowCallDurationThreshold(Duration.ofSeconds(3))
            .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before half-open
            .permittedNumberOfCallsInHalfOpenState(3)
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .build();
        
        this.circuitBreakerRegistry = CircuitBreakerRegistry.of(config);
        
        // Schedule cleanup every 30 seconds
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredRequests, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * Send request and wait for reply with Circuit Breaker protection
     * 
     * @param requestTopic Topic to send request
     * @param responseTopic Topic to receive response (for logging)
     * @param responseType Expected response class
     * @param requestFactory Creates request with correlationId
     * @param timeout Max time to wait for response
     * @return Response object
     */
    public <REQ, RESP> RESP request(
            String requestTopic,
            String responseTopic,
            Class<RESP> responseType,
            Function<String, REQ> requestFactory,
            Duration timeout) {
        
        String circuitBreakerName = "kafka-" + requestTopic;
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        
        return CircuitBreaker.decorateSupplier(circuitBreaker, () -> 
            doRequest(requestTopic, responseTopic, responseType, requestFactory, timeout)
        ).get();
    }

    /**
     * Request with fallback value on failure
     */
    public <REQ, RESP> RESP requestWithFallback(
            String requestTopic,
            String responseTopic,
            Class<RESP> responseType,
            Function<String, REQ> requestFactory,
            Duration timeout,
            RESP fallbackValue) {
        
        try {
            return request(requestTopic, responseTopic, responseType, requestFactory, timeout);
        } catch (Exception e) {
            log.warn("Request to {} failed, using fallback. Error: {}", requestTopic, e.getMessage());
            return fallbackValue;
        }
    }

    private <REQ, RESP> RESP doRequest(
            String requestTopic,
            String responseTopic,
            Class<RESP> responseType,
            Function<String, REQ> requestFactory,
            Duration timeout) {
        
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<Object> future = new CompletableFuture<>();
        
        // Register pending request
        pendingRequests.put(correlationId, future);
        
        try {
            // Create and send request
            REQ request = requestFactory.apply(correlationId);
            
            log.debug("Sending request to {} with correlationId={}", requestTopic, correlationId);
            
            CompletableFuture<SendResult<String, Object>> sendFuture = 
                kafkaTemplate.send(requestTopic, correlationId, request);
            
            // Wait for send confirmation
            sendFuture.get(5, TimeUnit.SECONDS);
            
            // Wait for response
            Object response = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            
            if (responseType.isInstance(response)) {
                return responseType.cast(response);
            } else {
                throw new IllegalStateException("Unexpected response type: " + 
                    response.getClass().getName() + ", expected: " + responseType.getName());
            }
            
        } catch (TimeoutException e) {
            log.error("Request timeout after {}ms for topic={}, correlationId={}", 
                timeout.toMillis(), requestTopic, correlationId);
            throw new KafkaRequestTimeoutException(
                "No response received within " + timeout.toMillis() + "ms", e);
            
        } catch (ExecutionException e) {
            log.error("Request execution failed for topic={}, correlationId={}", 
                requestTopic, correlationId, e.getCause());
            throw new KafkaRequestException("Request execution failed", e.getCause());
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaRequestException("Request interrupted", e);
            
        } finally {
            // Always cleanup
            pendingRequests.remove(correlationId);
        }
    }

    /**
     * Complete a pending request with response
     * Called by response listeners
     */
    public void complete(String correlationId, Object response) {
        CompletableFuture<Object> future = pendingRequests.get(correlationId);
        if (future != null) {
            future.complete(response);
            log.debug("Completed request with correlationId={}", correlationId);
        } else {
            log.warn("No pending request found for correlationId={}, response may be late or duplicate", 
                correlationId);
        }
    }

    /**
     * Complete a pending request with error
     */
    public void completeExceptionally(String correlationId, Throwable error) {
        CompletableFuture<Object> future = pendingRequests.get(correlationId);
        if (future != null) {
            future.completeExceptionally(error);
            log.debug("Completed request exceptionally with correlationId={}", correlationId);
        }
    }

    /**
     * Check if a request is pending
     */
    public boolean hasPendingRequest(String correlationId) {
        return pendingRequests.containsKey(correlationId);
    }

    /**
     * Get number of pending requests (for metrics)
     */
    public int getPendingRequestCount() {
        return pendingRequests.size();
    }

    /**
     * Cleanup expired/orphaned requests
     */
    private void cleanupExpiredRequests() {
        int cleaned = 0;
        for (Map.Entry<String, CompletableFuture<Object>> entry : pendingRequests.entrySet()) {
            CompletableFuture<Object> future = entry.getValue();
            // If future is done (completed, cancelled, or exceptionally completed)
            // but still in map, remove it
            if (future.isDone()) {
                pendingRequests.remove(entry.getKey());
                cleaned++;
            }
        }
        if (cleaned > 0) {
            log.debug("Cleaned up {} completed pending requests", cleaned);
        }
    }

    /**
     * Shutdown cleanup scheduler
     */
    public void shutdown() {
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Custom exceptions
    public static class KafkaRequestException extends RuntimeException {
        public KafkaRequestException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class KafkaRequestTimeoutException extends KafkaRequestException {
        public KafkaRequestTimeoutException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
