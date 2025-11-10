package com.ducnhu.common.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {

    // ================= PRODUCER =================
    @Bean
    public ProducerFactory<String, Object> producerFactory(Environment env) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("spring.kafka.bootstrap-servers"));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // giữ type header để tương thích (không bắt buộc với cách B)
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        props.put(ProducerConfig.RETRIES_CONFIG, 100);

        DefaultKafkaProducerFactory<String, Object> pf = new DefaultKafkaProducerFactory<>(props);
        boolean txEnabled = Boolean.parseBoolean(env.getProperty("ESHOP_KAFKA_TX_ENABLED", "false"));
        if (txEnabled) {
            pf.setTransactionIdPrefix(env.getProperty("ESHOP_KAFKA_TX_PREFIX", "eshop-tx-"));
        }
        return pf;
    }

    @Bean
    @Primary
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> pf) {
        return new KafkaTemplate<>(pf);
    }

    // ================= CONSUMER =================
    // 1) ConsumerFactory: giá trị raw là String
    @Bean
    public ConsumerFactory<String, String> consumerFactory(Environment env) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("spring.kafka.bootstrap-servers"));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        String gid = env.getProperty("spring.kafka.consumer.group-id",
                env.getProperty("spring.application.name", "eshop-default"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, gid);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    // 2) Converter: String JSON -> POJO theo kiểu tham số method @KafkaListener
    @Bean
    public RecordMessageConverter recordMessageConverter(ObjectMapper objectMapper) {
        return new JsonMessageConverter(objectMapper);
    }

    // 3) ContainerFactory mặc định: TÊN PHẢI LÀ "kafkaListenerContainerFactory"
    @Bean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            RecordMessageConverter recordMessageConverter) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setRecordMessageConverter(recordMessageConverter);

        factory.setBatchListener(false);
        factory.setConcurrency(5);
        // (tuỳ chọn) một vài tinh chỉnh hay dùng:
        // factory.getContainerProperties().setAckOnError(false);
        // factory.getContainerProperties().setObservationEnabled(false);

        return factory;
    }
}
