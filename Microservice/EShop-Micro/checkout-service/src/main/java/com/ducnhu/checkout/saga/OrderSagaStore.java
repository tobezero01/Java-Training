package com.ducnhu.checkout.saga;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OrderSagaStore {
    private final RedisTemplate<String, Object> redis;

    private String key(String orderNumber) {
        return "saga:order:" + orderNumber;
    }

    public void save(OrderSagaState s) {
        redis.opsForValue().set(key(s.getOrderNumber()), s, Duration.ofDays(1)); // TTL 1 ngày
    }
    public OrderSagaState get(String orderNumber) {
        Object o = redis.opsForValue().get(key(orderNumber));
        return (o instanceof OrderSagaState) ? (OrderSagaState) o : null;
    }
    public void delete(String orderNumber) { redis.delete(key(orderNumber)); }
}
