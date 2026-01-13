package com.ducnhu.payment.service;

import com.ducnhu.common.events.customer.AddressSnapshot;
import com.ducnhu.common.events.orders.OrderPaidEvent;
import com.ducnhu.common.events.orders.OrderPaidEventV2;
import com.ducnhu.common.events.orders.OrderPlacedItem;
import com.ducnhu.common.kafka.Topics;
import com.ducnhu.payment.dto.PaypalCaptureResult;
import com.ducnhu.payment.outbox.OutboxService;
import com.ducnhu.payment.saga.Payment;
import com.ducnhu.payment.saga.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentApplicationService {
    private final PaypalService paypal;
    private final PaymentRepository repo;
    private final OutboxService outbox;

    // NEW: lưu Payment ở trạng thái INTENT_CREATED khi tạo PayPal order
    public void createPendingPayment(String orderNumber,
                                     Integer customerId,
                                     String customerEmail,
                                     Float paymentTotal,
                                     String currency,
                                     String paypalOrderId) {

        Payment payment = repo.findByOrderNumber(orderNumber).orElseGet(Payment::new);
        payment.setOrderNumber(orderNumber);
        payment.setCustomerId(customerId);
        payment.setCustomerEmail(customerEmail);
        payment.setPaypalOrderId(paypalOrderId);
        payment.setAmount(paymentTotal);
        payment.setCurrency(currency);
        payment.setStatus("INTENT_CREATED");

        Instant now = Instant.now();
        if (payment.getCreatedAt() == null) {
            payment.setCreatedAt(now);
        }
        payment.setUpdatedAt(now);
        repo.save(payment);
    }

    // NEW: đánh dấu Payment bị huỷ (user cancel ở PayPal)
    public void markCancelled(String orderNumber) {
        repo.findByOrderNumber(orderNumber).ifPresent(p -> {
            p.setStatus("CANCELLED");
            p.setUpdatedAt(Instant.now());
            repo.save(p);
        });
    }

    public PaypalCaptureResult captureAndPublish(String paypalOrderId,
                                                 String orderNumber,
                                                 Integer customerId,
                                                 String customerEmail,
                                                 Float paymentTotal,
                                                 String currency,
                                                 AddressSnapshot shippingAddress,
                                                 Integer deliverDays,            // NEW
                                                 Date deliverDate ,
                                                 Float productTotal,
                                                 Float shippingCost,
                                                 List<OrderPlacedItem> items) {
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
                deliverDays, deliverDate,
                productTotal,
                shippingCost,
                items
        );
        outbox.enqueue(Topics.ORDER_PAID_EVENTS, orderNumber, event);
        return captureResult;
    }
}
