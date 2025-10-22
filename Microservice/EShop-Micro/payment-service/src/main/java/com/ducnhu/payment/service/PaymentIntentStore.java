package com.ducnhu.payment.service;

import com.ducnhu.payment.dto.Intent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class PaymentIntentStore {
    private final RedisTemplate<String,Object> redis;

    private String key(String orderNumber){ return "pi:paypal:"+orderNumber; }

    public void put(Intent i){ redis.opsForValue().set(key(i.orderNumber), i, Duration.ofMinutes(15)); }
    public Intent get(String orderNumber){
        Object o = redis.opsForValue().get(key(orderNumber));
        return (o instanceof Intent) ? (Intent)o : null;
    }
    public void remove(String orderNumber){ redis.delete(key(orderNumber)); }


}
