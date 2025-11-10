package com.ducnhu.payment.service;

import com.ducnhu.common.events.customer.AddressSnapshot;
import com.ducnhu.common.events.orders.OrderPaidEvent;
import com.ducnhu.common.events.orders.OrderPaidEventV2;
import com.ducnhu.common.kafka.Topics;
import com.ducnhu.payment.dto.PaypalCaptureResult;
import com.ducnhu.payment.outbox.OutboxService;
import com.ducnhu.payment.saga.Payment;
import com.ducnhu.payment.saga.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentApplicationService {
    private final PaypalService paypal;
    private final PaymentRepository repo;
    private final OutboxService outbox;

    public PaypalCaptureResult captureAndPublish(String paypalOrderId,
                                                 String orderNumber,
                                                 Integer customerId,
                                                 String customerEmail,
                                                 Float paymentTotal,
                                                 String currency,
                                                 AddressSnapshot shippingAddress,
                                                 Integer deliverDays,            // NEW
                                                 Date deliverDate ) {
        // goi capture
        PaypalCaptureResult captureResult = paypal.capture(paypalOrderId);

        // ghi Payment db
        Payment payment = repo.findByOrderNumber(orderNumber).orElseGet(Payment :: new);
        payment.setOrderNumber(orderNumber);
        payment.setCustomerId(customerId);
        payment.setCustomerEmail(customerEmail);
        payment.setPaypalOrderId(paypalOrderId);
        payment.setPaypalCaptureId(captureResult.captureId());
        payment.setStatus(captureResult.status());
        payment.setAmount(paymentTotal);
        payment.setCurrency(currency);
        payment.setUpdatedAt(Instant.now());
        repo.save(payment);

        OrderPaidEventV2 event = new OrderPaidEventV2(
                UUID.randomUUID().toString(),
                orderNumber, customerId, customerEmail,
                captureResult.captureId(), paymentTotal, currency, new Date(),
                shippingAddress, "PAYPAL",
                deliverDays, deliverDate
        );
        outbox.enqueue(Topics.ORDER_PAID_EVENTS, orderNumber, event);
        return captureResult;
    }
}
