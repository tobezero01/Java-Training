package com.ducnhu.payment.controller;

import com.ducnhu.common.dto.Intent;
import com.ducnhu.common.events.carts.CartClearCommand;
import com.ducnhu.common.events.customer.AddressDTO;
import com.ducnhu.common.events.customer.AddressQueryRequest;
import com.ducnhu.common.events.customer.AddressQueryResponse;
import com.ducnhu.common.events.customer.AddressSnapshot;
import com.ducnhu.common.events.orders.OrderCancelledEvent;
import com.ducnhu.common.events.settings.EmailSettingsRequest;
import com.ducnhu.common.events.settings.EmailSettingsResponse;
import com.ducnhu.common.events.shipping.ShippingRateRequest;
import com.ducnhu.common.events.shipping.ShippingRateResponse;
import com.ducnhu.common.kafka.RequestReplyClient;
import com.ducnhu.common.kafka.Topics;
import com.ducnhu.common.mail.CommonMailService;
import com.ducnhu.common.mail.MailUtil;
import com.ducnhu.payment.client.AuthClient;
import com.ducnhu.payment.client.MeResponse;
import com.ducnhu.payment.dto.PaypalCaptureResult;
import com.ducnhu.payment.dto.PaypalCreateResult;
import com.ducnhu.payment.dto.PaypalOrderValidation;
import com.ducnhu.payment.dto.Summary;
import com.ducnhu.payment.outbox.OutboxService;
import com.ducnhu.payment.service.PaymentApplicationService;
import com.ducnhu.payment.service.PaymentIntentStore;
import com.ducnhu.payment.service.PaymentOrchestrator;
import com.ducnhu.payment.service.PaypalService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/payments/paypal")
@RequiredArgsConstructor
@Slf4j
public class PaypalController {
    private final PaypalService paypalService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RequestReplyClient replyClient;
    private final CommonMailService mailService;

    private final PaymentOrchestrator orchestrator;
    private final PaymentIntentStore intentStore;

    private final PaymentApplicationService paymentApp;
    private final OutboxService outbox;

    private final AuthClient authClient;

    @PostMapping("/create")
    public Map<String, Object> create(
            @RequestParam(name = "addressId") Integer addressId,
            @RequestParam(name = "returnUrl") String returnUrl,
            @RequestParam(name = "cancelUrl") String cancelUrl,
            @AuthenticationPrincipal String customerEmail) {
        MeResponse me = authClient.me();
        AddressQueryResponse addr = replyClient.request(
                Topics.CUST_ADDR_REQ, Topics.CUST_ADDR_RESP, AddressQueryResponse.class,
                corr -> new AddressQueryRequest(corr, Topics.CUST_ADDR_RESP, me.id(), addressId),
                java.time.Duration.ofSeconds(10)
        );
        AddressDTO a = addr.address();
        if (a == null) throw new RuntimeException("Address not found");

// Chuẩn hoá field chống null
        String countryLabel = (a.countryName() != null && !a.countryName().isBlank()) ? a.countryName() : "Unknown";
        String line1 = (a.addressLine1() != null && !a.addressLine1().isBlank())
                ? a.addressLine1()
                : (a.addressLine2() != null ? a.addressLine2() : "N/A");

        AddressSnapshot adr = new AddressSnapshot(
                safe(a.firstName()), safe(a.lastName()), safe(a.phoneNumber()),
                line1, safe(a.addressLine2()),
                safe(a.city()), safe(a.state()), safe(a.postalCode()),
                countryLabel
        );
        // Tính tổng server-side
        Summary sum = orchestrator.summarize(me.id(), addressId);
        String currency = "USD";
        String orderNumber = "OD" + LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%06d", (int) (Instant.now().toEpochMilli() % 1_000_000));

        // 2) Tạo đơn PayPal bằng số tiền tính được + ép reference_id=orderNumber
        PaypalCreateResult r = paypalService.createOrderForServer(orderNumber, sum.paymentTotal(), currency, returnUrl, cancelUrl);
        Integer countryId = a.countryId();

        // 3) Lưu PaymentIntent để capture đối soát 1–1
        intentStore.put(new Intent(orderNumber, r.orderId(),
                sum.paymentTotal(), currency, me.id(),
                customerEmail, adr,
                countryId,
                addressId ));

        return Map.of(
                "paypalOrderId", r.orderId(),
                "approvalUrl", r.approvalUrl(),
                "orderNumber", orderNumber,
                "amount", sum.paymentTotal(),
                "currency", currency
        );
    }

    @PostMapping("/capture")
    public Map<String, Object> capture(@RequestParam("paypalOrderId") String paypalOrderId,
                                       @RequestParam(name = "orderNumber") String orderNumber) throws Exception {

        // 1) PaymentIntent
        Intent intent = intentStore.get(orderNumber);
        if (intent == null) {
            log.warn("REDIS: intent NOT FOUND for key pi:paypal:{}", orderNumber);
        } else if (!paypalOrderId.equals(intent.paypalOrderId)) {
            log.warn("REDIS: intent MISMATCH - redis.paypalOrderId={}, req.paypalOrderId={}",
                    intent.paypalOrderId, paypalOrderId);
        }
        if (intent == null || !paypalOrderId.equals(intent.paypalOrderId)) {
            throw new IllegalStateException("PaymentIntent not found or mismatched paypalOrderId");
        }

        AddressSnapshot sa = intent.getShippingAddress();
        Integer countryId = intent.getCountryId();
        String stateOrCity = (sa.state() != null && !sa.state().isBlank())
                ? sa.state().trim()
                : (sa.city() != null ? sa.city().trim() : "").replaceAll("\\s+", " ");

        ShippingRateResponse rate = replyClient.request(
                Topics.SHIP_RATE_REQ, Topics.SHIP_RATE_RESP, ShippingRateResponse.class,
                corr -> new ShippingRateRequest(corr, Topics.SHIP_RATE_RESP, countryId, stateOrCity),
                java.time.Duration.ofSeconds(3)
        );
        int deliverDays = (rate.days() == null) ? 0 : rate.days();
        ZoneId tz = ZoneId.of("Asia/Bangkok");
        LocalDate etaDate = LocalDate.now(tz).plusDays(deliverDays);

        Date deliverDate = Date.from(etaDate.atStartOfDay(tz).toInstant());
        String shippingAddressText = Stream.of(
                        (safe(sa.firstName()) + " " + safe(sa.lastName())).trim(),
                        safe(sa.line1()), safe(sa.line2()),
                        safe(sa.city()), safe(sa.state()), safe(sa.postalCode()), safe(sa.country())
                ).filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));
        String etaStr = (deliverDays > 0)
                ? etaDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : "Updating soon";
        PaypalCaptureResult cap = paymentApp.captureAndPublish(
                paypalOrderId,
                orderNumber,
                intent.customerId,
                intent.customerEmail,
                intent.amount,
                intent.currency,
                sa,
                deliverDays,
                deliverDate

        );

        PaypalOrderValidation validate = paypalService.validate(paypalOrderId, intent.amount, intent.currency, orderNumber, true);
        if (!validate.valid()) throw new IllegalStateException("Validation failed: " + validate.reason());

        EmailSettingsResponse settingsResponse = replyClient.request(
                Topics.SETTINGS_EMAIL_REQ, Topics.SETTINGS_EMAIL_RESP, EmailSettingsResponse.class,
                corr -> new EmailSettingsRequest(corr, Topics.SETTINGS_EMAIL_RESP),
                java.time.Duration.ofSeconds(3)
        );
        JavaMailSender sender = MailUtil.buildSender(settingsResponse.host(), settingsResponse.port(),
                settingsResponse.username(), settingsResponse.password(),
                settingsResponse.smtpAuth(), settingsResponse.smtpSecured());
        String orderTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String totalStr = String.format(Locale.US, "%.2f %s", intent.getAmount(), intent.getCurrency());
        String paymentMethod = "PayPal";
        String subj = settingsResponse.orderConfirmSubject().replace("[[orderId]]", orderNumber);
        String body = settingsResponse.orderConfirmContent()
                .replace("[[name]]", intent.getCustomerEmail())
                .replace("[[orderId]]", orderNumber)
                .replace("[[orderTime]]", orderTime)
                .replace("[[time]]", orderTime)
                .replace("[[shippingAddress]]", shippingAddressText)
                .replace("[[total]]", totalStr)
                .replace("[[paymentMethod]]", paymentMethod)
                .replace("[[deliveryDays]]", String.valueOf(deliverDays))
                .replace("[[deliveryDate]]", etaStr);

        mailService.sendHtml(sender, settingsResponse.mailFrom(), settingsResponse.senderName(),
                intent.getCustomerEmail(), subj, body);

        outbox.enqueue(Topics.CART_CLEAR_CMD, String.valueOf(intent.customerId),
                new CartClearCommand(intent.customerId));

        // 6) Xoá intent
        intentStore.remove(orderNumber);

        return Map.of("status", "COMPLETED", "captureId", cap.captureId(), "validation", validate);
    }

    @PostMapping("/cancel")
    public Map<String, Object> cancel(@RequestParam String orderNumber,
                                      @RequestParam(required = false) String paypalOrderId,
                                      @RequestParam(required = false, defaultValue = "Buyer cancelled at PayPal") String reason) throws JsonProcessingException {
        Intent intent = intentStore.get(orderNumber);
        if (intent != null && (paypalOrderId == null || paypalOrderId.equals(intent.paypalOrderId))) {

            // ENQUEUE sự kiện huỷ đơn (dual-write Outbox)
            outbox.enqueue(
                    Topics.ORDER_CANCELLED_EVENTS,
                    orderNumber,
                    new OrderCancelledEvent(
                            UUID.randomUUID().toString(),
                            orderNumber,
                            intent.customerId,
                            reason,
                            new Date()
                    )
            );

            intentStore.remove(orderNumber); // dọn intent
            return Map.of("cancelled", true, "orderNumber", orderNumber);
        }
        return Map.of("cancelled", false, "reason", "PaymentIntent not found or mismatched");
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String firstNonBlank(String... ss) {
        for (String s : ss) if (s != null && !s.isBlank()) return s;
        return null;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

}
