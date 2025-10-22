package com.ducnhu.payment.controller;

import com.ducnhu.common.events.carts.CartClearCommand;
import com.ducnhu.common.events.orders.OrderCancelledEvent;
import com.ducnhu.common.events.orders.OrderPaidEvent;
import com.ducnhu.common.events.settings.EmailSettingsRequest;
import com.ducnhu.common.events.settings.EmailSettingsResponse;
import com.ducnhu.common.kafka.RequestReplyClient;
import com.ducnhu.common.kafka.Topics;
import com.ducnhu.common.mail.CommonMailService;
import com.ducnhu.common.mail.MailUtil;
import com.ducnhu.payment.dto.*;
import com.ducnhu.payment.service.PaymentIntentStore;
import com.ducnhu.payment.service.PaymentOrchestrator;
import com.ducnhu.payment.service.PaypalService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments/paypal")
@RequiredArgsConstructor
public class PaypalController {
    private final PaypalService paypalService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RequestReplyClient replyClient;
    private final CommonMailService mailService;

    private final PaymentOrchestrator orchestrator;
    private final PaymentIntentStore intentStore;

    @PostMapping("/create")
    public Map<String, Object> create(@RequestParam String orderNumber,
                                      @RequestParam Integer addressId,
                                      @RequestParam String returnUrl,
                                      @RequestParam String cancelUrl,
                                      @AuthenticationPrincipal(expression = "claims['email']") String customerEmail,
                                      @AuthenticationPrincipal(expression = "claims['sub']") Integer customerId) {

        // 1) Tính tổng server-side
        Summary sum = orchestrator.summarize(customerId, addressId);
        String currency = "USD"; // hoặc lấy từ settings nếu bạn muốn

        // 2) Tạo đơn PayPal bằng số tiền tính được + ép reference_id=orderNumber
        PaypalCreateResult r = paypalService.createOrderForServer(orderNumber, sum.paymentTotal(), currency, returnUrl, cancelUrl);

        // 3) Lưu PaymentIntent để capture đối soát 1–1
        intentStore.put(new Intent(orderNumber, r.orderId(), sum.paymentTotal(), currency, customerId, customerEmail));

        return Map.of(
                "orderId", r.orderId(),
                "approvalUrl", r.approvalUrl(),
                "orderNumber", orderNumber,
                "amount", sum.paymentTotal(),
                "currency", currency
        );
    }

    @PostMapping("/capture")
    public Map<String, Object> capture(@RequestParam String paypalOrderId,
                                       @RequestParam String orderNumber) throws Exception {

        // 1) PaymentIntent
        Intent intent = intentStore.get(orderNumber);
        if (intent == null || !paypalOrderId.equals(intent.paypalOrderId)) {
            throw new IllegalStateException("PaymentIntent not found or mismatched paypalOrderId");
        }

        // 2) Capture từ PayPal
        PaypalCaptureResult cap = paypalService.capture(paypalOrderId);

        // 3) Validate tuyệt đối (server-side)
        PaypalOrderValidation v = paypalService.validate(paypalOrderId, intent.amount, intent.currency, orderNumber, true);
        if (!v.valid()) throw new IllegalStateException("Validation failed: " + v.reason());

        // 4) Gửi email xác nhận
        EmailSettingsResponse settingsResponse = replyClient.request(
                Topics.SETTINGS_EMAIL_REQ, Topics.SETTINGS_EMAIL_RESP, EmailSettingsResponse.class,
                corr -> new EmailSettingsRequest(corr, Topics.SETTINGS_EMAIL_RESP),
                java.time.Duration.ofSeconds(3)
        );
        JavaMailSender sender = MailUtil.buildSender(settingsResponse.host(), settingsResponse.port(),
                settingsResponse.username(), settingsResponse.password(),
                settingsResponse.smtpAuth(), settingsResponse.smtpSecured());
        String subj = settingsResponse.orderConfirmSubject().replace("[[orderId]]", orderNumber);
        String body = settingsResponse.orderConfirmContent()
                .replace("[[name]]", intent.customerEmail)
                .replace("[[orderId]]", orderNumber)
                .replace("[[time]]", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        mailService.sendHtml(sender, settingsResponse.mailFrom(), settingsResponse.senderName(), intent.customerEmail, subj, body);

        // 5) Dual-write an toàn: ORDER_PAID_EVENTS + CART_CLEAR_CMD trong 1 Kafka TX
        kafkaTemplate.executeInTransaction(kt -> {
            OrderPaidEvent paid = new OrderPaidEvent(
                    UUID.randomUUID().toString(), orderNumber,
                    intent.customerId, intent.customerEmail,
                    cap.captureId(), intent.amount, intent.currency, new Date()
            );
            kt.send(Topics.ORDER_PAID_EVENTS, orderNumber, paid); // key = orderNumber

            kt.send(Topics.CART_CLEAR_CMD, String.valueOf(intent.customerId),
                    new CartClearCommand(intent.customerId)); // key = customerId
            return true;
        });

        // 6) Xoá intent
        intentStore.remove(orderNumber);

        return Map.of("status", "COMPLETED", "captureId", cap.captureId(), "validation", v);
    }

    @PostMapping("/cancel")
    public Map<String, Object> cancel(@RequestParam String orderNumber,
                                      @RequestParam(required = false) String paypalOrderId,
                                      @RequestParam(required = false, defaultValue = "Buyer cancelled at PayPal") String reason) {
        Intent intent = intentStore.get(orderNumber);
        if (intent != null && (paypalOrderId == null || paypalOrderId.equals(intent.paypalOrderId))) {
            kafkaTemplate.send(Topics.ORDER_CANCELLED_EVENTS, orderNumber,
                    new OrderCancelledEvent(UUID.randomUUID().toString(), orderNumber, intent.customerId, reason, new Date()));
            intentStore.remove(orderNumber); // dọn intent
            return Map.of("cancelled", true, "orderNumber", orderNumber);
        }
        return Map.of("cancelled", false, "reason", "PaymentIntent not found or mismatched");
    }
}
