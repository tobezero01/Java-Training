package com.ducnhu.payment.controller;

import com.ducnhu.common.events.orders.OrderPaidEvent;
import com.ducnhu.common.events.settings.EmailSettingsRequest;
import com.ducnhu.common.events.settings.EmailSettingsResponse;
import com.ducnhu.common.kafka.RequestReplyClient;
import com.ducnhu.common.kafka.Topics;
import com.ducnhu.common.mail.CommonMailService;
import com.ducnhu.common.mail.MailUtil;
import com.ducnhu.payment.dto.PaypalValidation;
import com.ducnhu.payment.service.PaypalService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
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

    @PostMapping("/create")
    public Map<String, Object> create(@RequestParam String orderNumber,
                                      @RequestParam Float amount,
                                      @RequestParam String currency,
                                      @RequestParam String returnUrl,
                                      @RequestParam String cancelUrl) {
        return paypalService.createOrder(orderNumber, amount, currency, returnUrl, cancelUrl);
    }

    @PostMapping("/capture")
    public Map<String, Object> capture(@RequestParam String paypalOrderId,
                                       @RequestParam String customerEmail,
                                       @RequestParam String orderNumber,
                                       @RequestParam Float expectedAmount,
                                       @RequestParam String expectedCurrency) throws Exception {

        Map<String, Object> cap = paypalService.capture(paypalOrderId);
        PaypalValidation validate = paypalService.validate(paypalOrderId, expectedAmount, expectedCurrency);
        if (!"COMPLETED".equalsIgnoreCase(validate.status())) throw new RuntimeException("Payment not completed");

        // 1) Publish OrderPaidEvent
        OrderPaidEvent event = new OrderPaidEvent(
                UUID.randomUUID().toString(), orderNumber,
                null, customerEmail,
                validate.captureId(), validate.amount(),
                validate.currency(), Date.from(Instant.now())
        );
        kafkaTemplate.send(Topics.ORDER_PAID_EVENTS, event);

        // 2) Send mail xác nhận (ORDER_CONFIRMATION_*)
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
                .replace("[[name]]", customerEmail)
                .replace("[[orderId]]", orderNumber)
                .replace("[[time]]", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        mailService.sendHtml(sender, settingsResponse.mailFrom(),
                settingsResponse.senderName(), customerEmail, subj, body);

        return Map.of("status", "OK", "captureId", validate.captureId());
    }
}
