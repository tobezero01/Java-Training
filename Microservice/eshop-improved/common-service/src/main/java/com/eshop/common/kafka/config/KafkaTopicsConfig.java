package com.eshop.common.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka Topics Definition
 * 
 * Naming convention: {domain}.{entity}.{action}
 * DLQ topics: {original-topic}.DLQ
 */
@Configuration
public class KafkaTopicsConfig {

    @Value("${kafka.topic.partitions:3}")
    private int partitions;

    @Value("${kafka.topic.replication-factor:1}")
    private short replicationFactor;

    // ===================== CATALOG TOPICS =====================
    public static final String PRODUCT_SNAPSHOT_REQUEST = "catalog.product.snapshot.request";
    public static final String PRODUCT_SNAPSHOT_RESPONSE = "catalog.product.snapshot.response";
    public static final String PRODUCT_UPDATED = "catalog.product.updated";
    public static final String PRODUCT_PRICE_CHANGED = "catalog.product.price-changed";

    // ===================== CART TOPICS =====================
    public static final String CART_GET_REQUEST = "cart.items.get.request";
    public static final String CART_GET_RESPONSE = "cart.items.get.response";
    public static final String CART_CLEAR_COMMAND = "cart.items.clear.command";
    public static final String CART_RESERVE_COMMAND = "cart.items.reserve.command";
    public static final String CART_RELEASE_COMMAND = "cart.items.release.command";

    // ===================== CUSTOMER TOPICS =====================
    public static final String ADDRESS_QUERY_REQUEST = "customer.address.query.request";
    public static final String ADDRESS_QUERY_RESPONSE = "customer.address.query.response";

    // ===================== SHIPPING TOPICS =====================
    public static final String SHIPPING_RATE_REQUEST = "shipping.rate.query.request";
    public static final String SHIPPING_RATE_RESPONSE = "shipping.rate.query.response";

    // ===================== ORDER TOPICS =====================
    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_PAID = "order.paid";
    public static final String ORDER_CANCELLED = "order.cancelled";
    public static final String ORDER_SHIPPED = "order.shipped";
    public static final String ORDER_DELIVERED = "order.delivered";

    // ===================== PAYMENT TOPICS =====================
    public static final String PAYMENT_INITIATED = "payment.initiated";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String PAYMENT_REFUNDED = "payment.refunded";

    // ===================== SAGA TOPICS =====================
    public static final String SAGA_CHECKOUT_STARTED = "saga.checkout.started";
    public static final String SAGA_CHECKOUT_STEP_COMPLETED = "saga.checkout.step.completed";
    public static final String SAGA_CHECKOUT_COMPENSATE = "saga.checkout.compensate";

    // ===================== SETTINGS TOPICS =====================
    public static final String SETTINGS_EMAIL_REQUEST = "settings.email.query.request";
    public static final String SETTINGS_EMAIL_RESPONSE = "settings.email.query.response";
    public static final String SETTINGS_PAYPAL_REQUEST = "settings.paypal.query.request";
    public static final String SETTINGS_PAYPAL_RESPONSE = "settings.paypal.query.response";

    // ===================== TOPIC BEANS =====================
    // Auto-create topics on startup

    @Bean
    public NewTopic productSnapshotRequestTopic() {
        return TopicBuilder.name(PRODUCT_SNAPSHOT_REQUEST)
            .partitions(partitions)
            .replicas(replicationFactor)
            .config("retention.ms", "86400000") // 1 day
            .config("cleanup.policy", "delete")
            .build();
    }

    @Bean
    public NewTopic productSnapshotResponseTopic() {
        return TopicBuilder.name(PRODUCT_SNAPSHOT_RESPONSE)
            .partitions(partitions)
            .replicas(replicationFactor)
            .config("retention.ms", "3600000") // 1 hour (response doesn't need long retention)
            .build();
    }

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(ORDER_CREATED)
            .partitions(partitions)
            .replicas(replicationFactor)
            .config("retention.ms", "604800000") // 7 days
            .build();
    }

    @Bean
    public NewTopic orderPaidTopic() {
        return TopicBuilder.name(ORDER_PAID)
            .partitions(partitions)
            .replicas(replicationFactor)
            .config("retention.ms", "604800000")
            .build();
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name(ORDER_CANCELLED)
            .partitions(partitions)
            .replicas(replicationFactor)
            .config("retention.ms", "604800000")
            .build();
    }

    // DLQ topics
    @Bean
    public NewTopic orderCreatedDlqTopic() {
        return TopicBuilder.name(ORDER_CREATED + ".DLQ")
            .partitions(1) // DLQ usually needs fewer partitions
            .replicas(replicationFactor)
            .config("retention.ms", "2592000000") // 30 days for investigation
            .build();
    }

    @Bean
    public NewTopic orderPaidDlqTopic() {
        return TopicBuilder.name(ORDER_PAID + ".DLQ")
            .partitions(1)
            .replicas(replicationFactor)
            .config("retention.ms", "2592000000")
            .build();
    }

    @Bean
    public NewTopic sagaCheckoutStartedTopic() {
        return TopicBuilder.name(SAGA_CHECKOUT_STARTED)
            .partitions(partitions)
            .replicas(replicationFactor)
            .build();
    }

    @Bean
    public NewTopic sagaCompensateTopic() {
        return TopicBuilder.name(SAGA_CHECKOUT_COMPENSATE)
            .partitions(partitions)
            .replicas(replicationFactor)
            .build();
    }
}
