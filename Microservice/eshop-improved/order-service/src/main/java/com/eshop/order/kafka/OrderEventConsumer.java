package com.eshop.order.kafka;

import com.eshop.common.kafka.config.KafkaTopicsConfig;
import com.eshop.common.kafka.consumer.IdempotentConsumer;
import com.eshop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Order Event Consumer
 * 
 * Demonstrates idempotent consumption with manual acknowledgment.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final IdempotentConsumer idempotentConsumer;
    private final OrderService orderService;

    /**
     * Handle payment completed events
     */
    @KafkaListener(
        topics = KafkaTopicsConfig.PAYMENT_COMPLETED,
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentCompleted(
            ConsumerRecord<String, PaymentCompletedEvent> record,
            Acknowledgment acknowledgment) {
        
        String eventId = extractEventId(record);
        PaymentCompletedEvent event = record.value();
        
        log.info("Received PaymentCompletedEvent: eventId={}, orderNumber={}, amount={}",
            eventId, event.getOrderNumber(), event.getAmount());

        try {
            boolean processed = idempotentConsumer.processIdempotently(
                eventId,
                "PaymentCompletedEvent",
                "Order",
                event.getOrderNumber(),
                () -> {
                    // Business logic
                    orderService.markAsPaid(event.getOrderNumber(), event.getPaymentId());
                }
            );

            if (processed) {
                log.info("Successfully processed PaymentCompletedEvent for order {}", 
                    event.getOrderNumber());
            } else {
                log.info("Duplicate PaymentCompletedEvent for order {}, skipping", 
                    event.getOrderNumber());
            }

            // Acknowledge message after successful processing or duplicate detection
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process PaymentCompletedEvent for order {}: {}",
                event.getOrderNumber(), e.getMessage(), e);
            
            // Don't acknowledge - message will be retried
            // After max retries, it will go to DLQ
            throw e;
        }
    }

    /**
     * Handle order cancelled events
     */
    @KafkaListener(
        topics = KafkaTopicsConfig.ORDER_CANCELLED,
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleOrderCancelled(
            ConsumerRecord<String, OrderCancelledEvent> record,
            Acknowledgment acknowledgment) {
        
        String eventId = extractEventId(record);
        OrderCancelledEvent event = record.value();
        
        log.info("Received OrderCancelledEvent: eventId={}, orderNumber={}, reason={}",
            eventId, event.getOrderNumber(), event.getReason());

        try {
            idempotentConsumer.processIdempotently(
                eventId,
                "OrderCancelledEvent",
                "Order",
                event.getOrderNumber(),
                () -> orderService.handleCancellation(event.getOrderNumber(), event.getReason())
            );

            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process OrderCancelledEvent: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Handle DLQ messages for manual intervention
     */
    @KafkaListener(
        topics = KafkaTopicsConfig.PAYMENT_COMPLETED + ".DLQ",
        groupId = "${spring.kafka.consumer.group-id}-dlq"
    )
    public void handlePaymentCompletedDlq(
            ConsumerRecord<String, PaymentCompletedEvent> record,
            Acknowledgment acknowledgment) {
        
        String eventId = extractEventId(record);
        PaymentCompletedEvent event = record.value();
        
        log.error("DLQ message received: eventId={}, orderNumber={}, exception={}",
            eventId, 
            event.getOrderNumber(),
            extractHeader(record, "kafka_dlt-exception-message"));

        // Log for manual investigation
        // Could also store in database for admin dashboard
        // Or send alert to operations team

        acknowledgment.acknowledge();
    }

    /**
     * Extract event ID from Kafka headers
     */
    private String extractEventId(ConsumerRecord<?, ?> record) {
        return extractHeader(record, "eventId");
    }

    /**
     * Extract header value as string
     */
    private String extractHeader(ConsumerRecord<?, ?> record, String headerName) {
        var header = record.headers().lastHeader(headerName);
        if (header != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        // Fallback to record key if no eventId header
        return record.key() + "-" + record.offset();
    }

    // Event DTOs
    @lombok.Data
    public static class PaymentCompletedEvent {
        private String eventId;
        private String orderNumber;
        private String paymentId;
        private java.math.BigDecimal amount;
        private String paymentMethod;
        private java.time.Instant paidAt;
    }

    @lombok.Data
    public static class OrderCancelledEvent {
        private String eventId;
        private String orderNumber;
        private Integer customerId;
        private String reason;
        private java.time.Instant cancelledAt;
    }
}
