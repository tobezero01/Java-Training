package com.ducnhu.payment.saga;

import com.ducnhu.common.dto.Intent;
import com.ducnhu.common.events.orders.OrderCancelledEvent;
import com.ducnhu.common.kafka.Topics;
import com.ducnhu.payment.outbox.OutboxService;
import com.ducnhu.payment.service.PaymentIntentStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutJob {
    private final PaymentRepository paymentRepository;
    private final PaymentIntentStore intentStore;
    private final OutboxService outbox;

    // Thời gian hết hạn intent (nên khớp TTL Redis = 10 phút)
    @Value("${jobs.paymentIntentTimeout.minutes:10}")
    private long expireMinutes;

    @Value("${jobs.paymentIntentTimeout.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${jobs.paymentIntentTimeout.check-interval-ms:60000}")
    public void cancelExpiredIntents() {
        Instant threshold = Instant.now().minus(Duration.ofMinutes(expireMinutes));

        // Lấy tối đa batchSize payment đang INTENT_CREATED và đã quá hạn
        List<Payment> pending = paymentRepository
                .findTop100ByStatusAndCreatedAtBefore("INTENT_CREATED", threshold);

        for (Payment p : pending) {
            String orderNumber = p.getOrderNumber();
            try {
                Intent intent = intentStore.get(orderNumber);

                if (intent != null) {
                    continue;
                }

                log.info("Auto-cancel payment due to timeout, orderNumber={}", orderNumber);

                Integer customerId = p.getCustomerId();

                OrderCancelledEvent event = new OrderCancelledEvent(
                        UUID.randomUUID().toString(),
                        orderNumber,
                        customerId,
                        "PAYMENT_TIMEOUT",
                        new Date()
                );

                // enqueue sang Outbox để KafkaPublisher gửi
                outbox.enqueue(Topics.ORDER_CANCELLED_EVENTS, orderNumber, event);

                // cập nhật trạng thái payment
                p.setStatus("TIMED_OUT");
                p.setUpdatedAt(Instant.now());
                paymentRepository.save(p);

            } catch (JsonProcessingException e) {
                log.warn("Failed to load intent for orderNumber={} when timeout check", orderNumber, e);
            } catch (Exception ex) {
                log.warn("Error when auto-cancel orderNumber={} : {}", orderNumber, ex.getMessage(), ex);
            }
        }
    }
}
