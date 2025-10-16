package com.ducnhu.payment.service;

import com.ducnhu.common.events.settings.PaypalSettingsRequest;
import com.ducnhu.common.events.settings.PaypalSettingsResponse;
import com.ducnhu.common.kafka.RequestReplyClient;
import com.ducnhu.common.kafka.Topics;
import com.ducnhu.payment.dto.PaypalValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaypalService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final RequestReplyClient replyClient;

    private PaypalSettingsResponse settings() {
        return replyClient.request(
                Topics.SETTINGS_PAYPAL_REQ, Topics.SETTINGS_PAYPAL_RESP, PaypalSettingsResponse.class,
                corr -> new PaypalSettingsRequest(corr, Topics.SETTINGS_PAYPAL_RESP),
                Duration.ofSeconds(3)
        );
    }

    private String accessToken(PaypalSettingsResponse paypalSettingsResponse) {
        String url = paypalSettingsResponse.baseUrl() + "/v1/oauth2/token";
        String credentials = paypalSettingsResponse.clientId() + " " + paypalSettingsResponse.clientSecret();
        String basic = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", basic);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST,
                new HttpEntity<>(form, headers), Map.class);
        Object token = response.getBody().get("access_token");
        if (token == null) throw new RuntimeException("No access_token from PayPal");
        return token.toString();
    }

    public Map<String, Object> createOrder(String reference, Float amount, String currency,
                                           String returnURL, String cancelURL) {
        PaypalSettingsResponse bag = settings();
        String token = accessToken(bag);

        String url = bag.baseUrl() + "/v2/checkout/orders";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "intent", "CAPTURE",
                "purchase_units", List.of(Map.of(
                        "reference_id", reference,
                        "amount", Map.of("currency_code", currency, "value", String.format(Locale.US, "%.2f", amount))
                )),
                "application_context", Map.of(
                        "return_url", returnURL, "cancel_url", cancelURL,
                        "user_action", "PAY_NOW", "brand_name", "EShop", "landing_page", "LOGIN", "shipping_preference", "NO_SHIPPING"
                )
        );

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        return response.getBody();

    }

    public Map<String, Object> capture(String paypalOrderId) {
        PaypalSettingsResponse bag = settings();
        String token = accessToken(bag);
        String url = bag.baseUrl() + "/v2/checkout/orders/" + paypalOrderId + "/capture";
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(Map.of(), h), Map.class);
        return resp.getBody();
    }

    public Map<String, Object> getOrder(String paypalOrderId) {
        PaypalSettingsResponse bag = settings();
        String token = accessToken(bag);
        String url = bag.baseUrl() + "/v2/checkout/orders/" + paypalOrderId;
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(h), Map.class);
        return resp.getBody();
    }

    public PaypalValidation validate(String paypalOrderId, Float expectedAmount, String expectedCurrency) {
        Map<String, Object> ord = getOrder(paypalOrderId);
        String status = String.valueOf(ord.get("status")); // COMPLETED sau capture
        List<Map<String, Object>> pu = (List<Map<String, Object>>) ord.get("purchase_units");
        Map<String, Object> u0 = pu.get(0);
        Map<String, Object> amt = (Map<String, Object>) u0.get("amount");
        String value = String.valueOf(amt.get("value"));
        String currency = String.valueOf(amt.get("currency_code"));
        String ref = String.valueOf(u0.get("reference_id"));
        String capId = null;
        if (ord.get("purchase_units") != null) {
            List<Map<String, Object>> caps = (List<Map<String, Object>>) ((Map<String, Object>) u0.get("payments")).get("captures");
            if (caps != null && !caps.isEmpty()) capId = String.valueOf(caps.get(0).get("id"));
        }
        Float val = Float.valueOf(value);
        if (!Objects.equals(currency, expectedCurrency)) throw new RuntimeException("Currency mismatch");
        if (Math.abs(val - expectedAmount) > 0.01f) throw new RuntimeException("Amount mismatch");
        return new PaypalValidation(status, val, currency, ref, capId);
    }

}
