package com.ducnhu.payment.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {
    @Value("${outbox.batch-size:50}")
    private int batchSize;
    @Value("${outbox.publish-interval-ms:2000}")
    private long intervalMs;

    private final OutboxRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper mapper;

    @Scheduled(fixedDelayString = "${outbox.publish-interval-ms}")
    @Transactional
    public void publishBatch() {
        List<OutboxMessage> batch = repository.pickPendingForUpdate(Instant.now(), PageRequest.of(0, batchSize));

        for (OutboxMessage outboxSms : batch) {
            try {
                outboxSms.setStatus(OutboxStatus.IN_PROGRESS);
                outboxSms.setLockedAt(Instant.now());
                outboxSms.setLockedBy(hostname());

                // Deserialize payload theo 'type'
                Class<?> tClass = Class.forName(outboxSms.getType());

                // Gửi Kafka (JSON Serializer đã cấu hình trong project)
                Object event = mapper.readValue(outboxSms.getPayload(), tClass);

                kafkaTemplate.send(outboxSms.getTopic(), outboxSms.getKey(), event).get();

                outboxSms.setStatus(OutboxStatus.PUBLISHED);
                outboxSms.setPublishedAt(Instant.now());
                outboxSms.setLastError(null);
            } catch (Exception exception) {
                outboxSms.setStatus(OutboxStatus.FAILED);
                outboxSms.setAttempts(outboxSms.getAttempts() + 1);
                outboxSms.setLastError(shorten(exception.getMessage(), 1900));

                long delaySec = (long) Math.min(60, Math.pow(2, Math.max(1, outboxSms.getAttempts())));
                outboxSms.setNextAttemptAt(Instant.now().plusSeconds(delaySec));
                log.warn("Outbox send failed id={}, attempt={}, cause={}",
                        outboxSms.getId(), outboxSms.getAttempts(), outboxSms.toString());

            }
        }
    }

    private static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String shorten(String s, int max) {
        if (s == null) return null;
        return (s.length() <= max) ? s : s.substring(0, max);
    }
}
