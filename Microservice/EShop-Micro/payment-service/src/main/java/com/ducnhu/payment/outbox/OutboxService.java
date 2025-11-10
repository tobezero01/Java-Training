package com.ducnhu.payment.outbox;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxService {
    private final ObjectMapper objectMapper;
    private final OutboxRepository outboxRepository;

    @Transactional
    public void enqueue(String topic, String key, Object event) {
        try {
            OutboxMessage message = new OutboxMessage();
            message.setTopic(topic);
            message.setKey(key);
            message.setType(event.getClass().getName());
            message.setPayload(objectMapper.writeValueAsString(event));
            message.setStatus(OutboxStatus.PENDING);
            outboxRepository.save(message);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException(exception);
        }
    }
}
