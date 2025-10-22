package com.ducnhu.common.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class RequestReplyClient {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private final ConcurrentMap<String, CompletableFuture<Object>> waiters = new ConcurrentHashMap<>();

    /**
     * Gửi request (payload có correlationId, replyTo) và đợi response qua topic reply.
     */
    public <TReq, TResp> TResp request(String topic, String replyTopic, Class<TResp> respType,
                                       Function<String, TReq> build, Duration timeout) {
        String corr = UUID.randomUUID().toString();

        TReq payload = build.apply(corr);
        CompletableFuture<Object> future = new CompletableFuture<>();
        waiters.put(corr, future);
        kafkaTemplate.send(topic, payload);

        try {
            Object resp = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return objectMapper.convertValue(resp, respType);
        } catch (TimeoutException e) {
            waiters.remove(corr);
            throw new RuntimeException("Timeout waiting reply from " + topic);
        } catch (Exception e) {
            waiters.remove(corr);
            throw new RuntimeException(e);
        }
    }

    /**
     * Receiver chung cho các reply topics — đăng ký @KafkaListener ở service cụ thể.
     */
    public void complete(String correlationId, Object resp) {
        CompletableFuture<Object> fut = waiters.remove(correlationId);
        if (fut != null) fut.complete(resp);
    }
}
