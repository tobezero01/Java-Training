package com.ducnhu.checkout.saga;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class OrderSagaService {
    private final OrderSagaRepository orderSagaRepository;
    private final OrderSagaStore orderSagaStore;

    @Transactional
    public void createNew(String orderNumber, Integer customerId) {
        // lưu db
        OrderSagaEntity event = new OrderSagaEntity();
        event.setOrderNumber(orderNumber);
        event.setCustomerId(customerId);
        event.setStatus(OrderSagaEntity.Status.NEW);
        event.setUpdatedAt(Instant.now());
        orderSagaRepository.save(event);

        // lưu với redis
        OrderSagaState sagaRedis = new OrderSagaState();
        sagaRedis.setOrderNumber(orderNumber);
        sagaRedis.setCustomerId(customerId);
        sagaRedis.setStatus(OrderSagaState.Status.NEW);
        sagaRedis.setUpdatedAt(new Date());
        orderSagaStore.save(sagaRedis);
    }

    @Transactional
    public void mark(String orderNumber, OrderSagaEntity.Status status, String note) {
        OrderSagaEntity entity = orderSagaRepository.findByOrderNumber(orderNumber).orElseThrow();
        entity.setStatus(status);
        entity.setUpdatedAt(Instant.now());
        entity.setNote(note);
        orderSagaRepository.save(entity);

        OrderSagaState state = orderSagaStore.get(orderNumber);
        if (state != null) {
            state.setStatus(switch (status) {
                case NEW -> OrderSagaState.Status.NEW;
                case PUBLISHED -> OrderSagaState.Status.PUBLISHED;
                case COMPLETED -> OrderSagaState.Status.COMPLETED;
                case CANCELLED -> OrderSagaState.Status.CANCELLED;
            });
            state.setUpdatedAt(new Date());
            state.setNote(note);
            orderSagaStore.save(state);
        }
    }
}
