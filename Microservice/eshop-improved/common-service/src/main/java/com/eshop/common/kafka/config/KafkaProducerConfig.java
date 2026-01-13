package com.eshop.common.kafka.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Production-grade Kafka Producer Configuration
 * 
 * Key improvements:
 * 1. Idempotent producer (exactly-once semantics)
 * 2. Proper acknowledgment (acks=all)
 * 3. Retry with backoff
 * 4. Compression for better throughput
 * 5. Batch settings for performance
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.producer.idempotence:true}")
    private boolean enableIdempotence;

    @Value("${kafka.producer.retries:3}")
    private int retries;

    @Value("${kafka.producer.batch-size:16384}")
    private int batchSize;

    @Value("${kafka.producer.linger-ms:5}")
    private int lingerMs;

    @Value("${kafka.producer.compression-type:snappy}")
    private String compressionType;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        
        // Basic settings
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // === PRODUCTION SETTINGS ===
        
        // 1. Idempotent producer - prevents duplicate messages on retry
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, enableIdempotence);
        
        // 2. Acknowledgments - wait for all replicas to acknowledge
        // acks=all ensures message is written to all in-sync replicas
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        
        // 3. Retries with exponential backoff
        props.put(ProducerConfig.RETRIES_CONFIG, retries);
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100);
        
        // 4. Max in-flight requests - set to 5 for idempotent producer
        // This ensures ordering with exactly-once semantics
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        
        // 5. Batching for better throughput
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        props.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        
        // 6. Compression - snappy is good balance of CPU and compression ratio
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
        
        // 7. Buffer memory for pending records
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB
        
        // 8. Request timeout and delivery timeout
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        
        // 9. JSON serializer settings - include type info for deserialization
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory());
        // Set default topic for error handling
        template.setObservationEnabled(true); // Enable micrometer metrics
        return template;
    }
}
