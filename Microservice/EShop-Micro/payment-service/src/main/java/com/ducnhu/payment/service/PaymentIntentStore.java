package com.ducnhu.payment.service;

import com.ducnhu.common.dto.Intent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class PaymentIntentStore {

    // Khuyến nghị thêm @Qualifier để chắc chắn lấy đúng bean typed
    @Qualifier("intentRedisTemplate")
    private final RedisTemplate<String, com.ducnhu.common.dto.Intent> redis;

    private final ObjectMapper objectMapper; // fallback chuyển Map → Intent

    private String key(String orderNumber) {
        return "pi:paypal:" + orderNumber;
    }

    public void put(Intent i) {
        redis.opsForValue().set(key(i.getOrderNumber()), i, Duration.ofMinutes(15));
    }

    public Intent get(String orderNumber) throws JsonProcessingException {
        // Dù template typed, nếu lỡ còn dữ liệu cũ trong Redis, vẫn có thể trả Map.
        Object raw = redis.opsForValue().get(key(orderNumber));
        if (raw == null) return null;
        if (raw instanceof Intent i) return i;
        if (raw instanceof java.util.Map<?, ?> m) return objectMapper.convertValue(m, Intent.class);
        if (raw instanceof String s) return objectMapper.readValue(s, Intent.class);
        throw new IllegalStateException("Unsupported intent value type: " + raw.getClass());
    }

    public void remove(String orderNumber) {
        redis.delete(key(orderNumber));
    }
}


//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class PaymentIntentStore {
//    private final RedisTemplate<String,Object> redis;
//    private final ObjectMapper mapper;
//
//    private String key(String orderNumber){ return "pi:paypal:" + orderNumber; }
//
//    public void put(Intent i){ redis.opsForValue().set(key(i.orderNumber()), i, Duration.ofMinutes(15)); }
//
//    public Intent get(String orderNumber){
//        Object o = redis.opsForValue().get(key(orderNumber));
//        if (o == null) return null;
//        if (o instanceof Intent i) return i;
//        if (o instanceof String s) return mapper.readValue(s, Intent.class);
//        if (o instanceof Map m)    return mapper.convertValue(m, Intent.class);
//        log.warn("Unsupported intent value type: {}", o.getClass());
//        return null;
//    }
//}
//@Component
//@RequiredArgsConstructor
//public class PaymentIntentStore {
//    private final RedisTemplate<String,Object> redis;
//
//    private String key(String orderNumber){ return "pi:paypal:"+orderNumber; }
//
//    public void put(Intent i){ redis.opsForValue().set(key(i.orderNumber), i, Duration.ofMinutes(5)); }
//    public Intent get(String orderNumber){
//        Object o = redis.opsForValue().get(key(orderNumber));
//        return (o instanceof Intent) ? (Intent)o : null;
//    }
//    public void remove(String orderNumber){ redis.delete(key(orderNumber)); }
//
//
//}
