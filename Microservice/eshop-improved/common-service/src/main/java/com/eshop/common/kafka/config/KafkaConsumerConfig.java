package com.eshop.common.kafka.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Production-grade Kafka Consumer Configuration
 * 
 * Key improvements:
 * 1. Dead Letter Queue (DLQ) for failed messages
 * 2. Exponential backoff retry
 * 3. Error handling deserializer
 * 4. Configurable concurrency
 * 5. Manual offset commit for reliability
 */
@Configuration
@EnableKafka
@Slf4j
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${kafka.consumer.concurrency:3}")
    private int concurrency;

    @Value("${kafka.consumer.max-retries:3}")
    private int maxRetries;

    @Value("${kafka.consumer.retry-interval-ms:1000}")
    private long retryInterval;

    @Value("${kafka.consumer.max-retry-interval-ms:10000}")
    private long maxRetryInterval;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        
        // Basic settings
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        
        // === ERROR HANDLING DESERIALIZER ===
        // Wraps JsonDeserializer to handle deserialization errors gracefully
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        
        // JSON deserializer settings
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.eshop.*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);
        
        // === PRODUCTION SETTINGS ===
        
        // 1. Auto offset reset - earliest to not miss messages on new consumer
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        // 2. MANUAL commit for reliability (we commit after successful processing)
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        // 3. Session timeout - if no heartbeat, consumer is considered dead
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        
        // 4. Heartbeat interval - should be 1/3 of session timeout
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        
        // 5. Max poll records - control batch size per poll
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        
        // 6. Max poll interval - max time between polls before consumer is kicked
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        
        // 7. Fetch settings for throughput
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        
        // 8. Isolation level - read_committed for transactional messages
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Producer factory for DLQ publishing
     */
    @Bean
    public ProducerFactory<String, Object> dlqProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> dlqKafkaTemplate() {
        return new KafkaTemplate<>(dlqProducerFactory());
    }

    /**
     * Dead Letter Queue recoverer
     * Failed messages after all retries go to {original-topic}.DLQ
     */
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer() {
        return new DeadLetterPublishingRecoverer(dlqKafkaTemplate(),
            (record, exception) -> {
                // Send to DLQ topic with ".DLQ" suffix
                String dlqTopic = record.topic() + ".DLQ";
                log.error("Sending to DLQ: {} | Original topic: {} | Key: {} | Error: {}",
                    dlqTopic, record.topic(), record.key(), exception.getMessage());
                return new org.apache.kafka.common.TopicPartition(dlqTopic, record.partition());
            });
    }

    /**
     * Error handler with exponential backoff and DLQ
     */
    @Bean
    public CommonErrorHandler errorHandler() {
        // Exponential backoff: 1s, 2s, 4s, 8s... up to maxRetryInterval
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(maxRetries);
        backOff.setInitialInterval(retryInterval);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(maxRetryInterval);
        
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            deadLetterPublishingRecoverer(),
            backOff
        );
        
        // Log retry attempts
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("Retry attempt {} for topic={}, partition={}, offset={}, error={}",
                deliveryAttempt, record.topic(), record.partition(), record.offset(), ex.getMessage());
        });
        
        // Don't retry on deserialization errors - send directly to DLQ
        errorHandler.addNotRetryableExceptions(
            org.apache.kafka.common.errors.SerializationException.class,
            org.springframework.messaging.converter.MessageConversionException.class
        );
        
        return errorHandler;
    }

    /**
     * Main Kafka listener container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(errorHandler());
        
        // Concurrency - number of consumer threads per listener
        factory.setConcurrency(concurrency);
        
        // MANUAL_IMMEDIATE - commit offset immediately after processing each record
        factory.getContainerProperties().setAckMode(
            org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE
        );
        
        // Enable batch listener for high throughput scenarios
        // factory.setBatchListener(true);
        
        // Observation for metrics
        factory.getContainerProperties().setObservationEnabled(true);
        
        return factory;
    }

    /**
     * Separate factory for batch processing (high throughput)
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> batchKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(errorHandler());
        factory.setConcurrency(concurrency);
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(
            org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL
        );
        
        return factory;
    }
}
