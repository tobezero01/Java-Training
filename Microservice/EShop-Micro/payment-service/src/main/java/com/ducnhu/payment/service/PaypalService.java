package com.ducnhu.payment.service;

import com.ducnhu.common.events.settings.PaypalSettingsRequest;
import com.ducnhu.common.events.settings.PaypalSettingsResponse;
import com.ducnhu.common.kafka.RequestReplyClient;
import com.ducnhu.common.kafka.Topics;
import com.ducnhu.payment.dto.PaypalCaptureResult;
import com.ducnhu.payment.dto.PaypalCreateResult;
import com.ducnhu.payment.dto.PaypalOrderValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaypalService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final RequestReplyClient replyClient;

    private PaypalSettingsResponse settings() {
        return replyClient.request(
                Topics.SETTINGS_PAYPAL_REQ,
                Topics.SETTINGS_PAYPAL_RESP,
                PaypalSettingsResponse.class,
                corr -> new PaypalSettingsRequest(corr, Topics.SETTINGS_PAYPAL_RESP),
                Duration.ofSeconds(4)
        );
    }

    private String accessToken(PaypalSettingsResponse cfg) {
        String url = cfg.baseUrl() + "/v1/oauth2/token";
        String credentials = cfg.clientId() + ":" + cfg.clientSecret();                        // chuẩn Basic: id:secret
        String basic = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", basic);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");

        try {
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(form, headers), Map.class);
            Map body = Optional.ofNullable(resp.getBody()).orElse(Map.of());
            Object token = body.get("access_token");
            if (token == null) throw new RuntimeException("No access_token from PayPal");
            return String.valueOf(token);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("OAuth2 failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        } catch (Exception ex) {
            throw new RuntimeException("OAuth2 failed: " + ex.getMessage(), ex);
        }
    }

    public PaypalCreateResult createOrder(String referenceId, Float amount, String currency,
                                          String returnUrl, String cancelUrl) {
        PaypalSettingsResponse cfg = settings();
        String token = accessToken(cfg);

        String url = cfg.baseUrl() + "/v2/checkout/orders";
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "intent", "CAPTURE",
                "purchase_units", List.of(Map.of(
                        "reference_id", referenceId,
                        "amount", Map.of("currency_code", currency, "value", String.format(Locale.US, "%.2f", amount))
                )),
                "application_context", Map.of(
                        "return_url", returnUrl, "cancel_url", cancelUrl,
                        "user_action", "PAY_NOW", "brand_name", "EShop", "landing_page", "LOGIN", "shipping_preference", "NO_SHIPPING"
                )
        );

        try {
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, h), Map.class);
            Map<String, Object> map = ensure(resp.getBody());
            String id = map.get("id") != null ? String.valueOf(map.get("id")) : null;
            String approval = extractApprovalUrl(map);
            return new PaypalCreateResult(id, approval, map);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Create order failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        } catch (Exception ex) {
            throw new RuntimeException("Create order failed: " + ex.getMessage(), ex);
        }
    }

    public PaypalCreateResult createOrderForServer(String orderNumber, float paymentTotal, String currency,
                                                   String returnUrl, String cancelUrl) {
        PaypalSettingsResponse cfg = settings();
        String token = accessToken(cfg);

        String url = cfg.baseUrl() + "/v2/checkout/orders";
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "intent", "CAPTURE",
                "purchase_units", List.of(Map.of(
                        "reference_id", orderNumber,
                        "amount", Map.of("currency_code", currency, "value", String.format(Locale.US, "%.2f", paymentTotal))
                )),
                "application_context", Map.of(
                        "return_url", returnUrl, "cancel_url", cancelUrl,
                        "user_action", "PAY_NOW", "brand_name", "EShop", "landing_page", "LOGIN", "shipping_preference", "NO_SHIPPING"
                )
        );

        try {
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, h), Map.class);
            Map<String, Object> map = ensure(resp.getBody());
            String id = map.get("id") != null ? String.valueOf(map.get("id")) : null;
            String approval = extractApprovalUrl(map);
            return new PaypalCreateResult(id, approval, map);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Create order failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        } catch (Exception ex) {
            throw new RuntimeException("Create order failed: " + ex.getMessage(), ex);
        }
    }

    public PaypalCaptureResult capture(String paypalOrderId) {
        PaypalSettingsResponse cfg = settings();
        String token = accessToken(cfg);

        String url = cfg.baseUrl() + "/v2/checkout/orders/" + paypalOrderId + "/capture";
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(Map.of(), h), Map.class);
            Map<String, Object> map = ensure(resp.getBody());
            String captureId = extractFirstCaptureId(map);
            String status = str(map.get("status")); // PayPal trả COMPLETED nếu capture thành công

            return new PaypalCaptureResult(captureId, status, map);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Capture failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        } catch (Exception ex) {
            throw new RuntimeException("Capture failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Get order JSON (sử dụng lại để validate)
     */
    public Map<String, Object> getOrder(String paypalOrderId) {
        PaypalSettingsResponse cfg = settings();
        String token = accessToken(cfg);
        String url = cfg.baseUrl() + "/v2/checkout/orders/" + paypalOrderId;

        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(h), Map.class);
            return ensure(resp.getBody());
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Get order failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
        } catch (Exception ex) {
            throw new RuntimeException("Get order failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Validate order: so khớp số tiền/đơn vị tiền/tình trạng và (tuỳ chọn) referenceId.
     *
     * @param requireCompleted nếu true: status phải COMPLETED (sau capture); nếu false: APPROVED cũng chấp nhận (trước capture)
     */
    public PaypalOrderValidation validate(String paypalOrderId,
                                          Float expectedAmount,
                                          String expectedCurrency,
                                          String expectedReferenceId,
                                          boolean requireCompleted) {
        Map<String, Object> ord = getOrder(paypalOrderId);

        String status = str(ord.get("status"));        // APPROVED | COMPLETED ...
        String intent = str(ord.get("intent"));        // CAPTURE | AUTHORIZE
        Map<String, Object> pu0 = firstPurchaseUnit(ord);
        Map<String, Object> amt = (pu0 == null) ? null : asMap(pu0.get("amount"));

        String currency = (amt == null) ? null : str(amt.get("currency_code"));
        Float amount = parseFloat((amt == null) ? null : amt.get("value"));

        String refId = (pu0 == null) ? null : str(pu0.get("reference_id"));
        String captureId = extractFirstCaptureId(ord);

        Map<String, Object> payer = asMap(ord.get("payer"));
        String payerEmail = (payer == null) ? null : str(payer.get("email_address"));
        String payerId = (payer == null) ? null : str(payer.get("payer_id"));

        boolean statusOk = requireCompleted
                ? "COMPLETED".equalsIgnoreCase(status)
                : ("APPROVED".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status));

        boolean amountOk = (expectedAmount == null) || (amount != null && Math.abs(amount - expectedAmount) <= 0.01f);
        boolean currencyOk = (expectedCurrency == null) || (currency != null && expectedCurrency.equalsIgnoreCase(currency));
        boolean referenceOk = (expectedReferenceId == null) || (refId != null && expectedReferenceId.equals(refId));

        boolean valid = statusOk && amountOk && currencyOk && referenceOk;
        String reason = valid ? null : ("Invalid: " + explain(statusOk, amountOk, currencyOk, referenceOk,
                Map.of("status", status, "amount", amount, "currency", currency, "referenceId", refId)));

        return new PaypalOrderValidation(
                paypalOrderId, intent, status, currency, amount, refId, captureId,
                payerEmail, payerId, valid, statusOk, amountOk, currencyOk, referenceOk, reason
        );
    }

    private static Map<String, Object> ensure(Map body) {
        return (body == null) ? Map.of() : body;
    }

    private static String str(Object o) {
        return (o == null) ? null : String.valueOf(o);
    }

    private static String extractApprovalUrl(Map<String, Object> map) {
        List<Map<String, Object>> links = asList(map.get("links"));
        return links.stream()
                .filter(l -> "approve".equalsIgnoreCase(str(l.get("rel"))))
                .map(l -> str(l.get("href"))).filter(Objects::nonNull).findFirst().orElse(null);
    }

    /**
     * lấy capture id đầu tiên nếu đã capture (trong purchase_units[0].payments.captures[0].id)
     */
    private static String extractFirstCaptureId(Map<String, Object> ord) {
        Map<String, Object> pu0 = firstPurchaseUnit(ord);
        if (pu0 == null) return null;
        Map<String, Object> payments = asMap(pu0.get("payments"));
        if (payments == null) return null;
        List<Map<String, Object>> captures = asList(payments.get("captures"));
        if (captures.isEmpty()) return null;
        return str(captures.get(0).get("id"));
    }

    /**
     * lấy purchase_units[0] một cách an toàn
     */
    private static Map<String, Object> firstPurchaseUnit(Map<String, Object> ord) {
        List<Map<String, Object>> pus = asList(ord.get("purchase_units"));
        return pus.isEmpty() ? null : pus.get(0);
    }

    private static String explain(boolean statusOk, boolean amountOk, boolean currencyOk, boolean referenceOk,
                                  Map<String, Object> snapshot) {
        String flags = List.of(
                statusOk ? null : "status",
                amountOk ? null : "amount",
                currencyOk ? null : "currency",
                referenceOk ? null : "referenceId"
        ).stream().filter(Objects::nonNull).collect(Collectors.joining(", "));
        return "mismatch at [" + flags + "], snapshot=" + snapshot;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return (o instanceof Map) ? (Map<String, Object>) o : null;
    }

    private static Float parseFloat(Object o) {
        try {
            return (o == null) ? null : Float.parseFloat(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> asList(Object o) {
        return (o instanceof List) ? (List<Map<String, Object>>) o : List.of();
    }

}
