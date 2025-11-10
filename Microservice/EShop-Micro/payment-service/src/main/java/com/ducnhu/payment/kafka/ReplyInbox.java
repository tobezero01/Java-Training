package com.ducnhu.payment.kafka;

import com.ducnhu.common.events.carts.CartGetResponse;
import com.ducnhu.common.events.catalog.ProductSnapshotResponse;
import com.ducnhu.common.events.customer.AddressQueryResponse;
import com.ducnhu.common.events.settings.EmailSettingsResponse;
import com.ducnhu.common.events.settings.PaypalSettingsResponse;
import com.ducnhu.common.events.shipping.ShippingRateResponse;
import com.ducnhu.common.kafka.RequestReplyClient;
import com.ducnhu.common.kafka.Topics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReplyInbox {

    private final RequestReplyClient requestReplyClient;

    // Nhận cấu hình PayPal cho PaypalService.settings()
    @KafkaListener(topics = Topics.SETTINGS_PAYPAL_RESP, groupId = "payment-service")
    public void onPaypal(PaypalSettingsResponse resp) {
        requestReplyClient.complete(resp.correlationId(), resp);
    }

    @KafkaListener(topics = Topics.SETTINGS_EMAIL_RESP, groupId = "payment-service")
    public void onEmail(EmailSettingsResponse resp) {
        requestReplyClient.complete(resp.correlationId(), resp);
    }

    @KafkaListener(topics = Topics.CART_GET_RESP, groupId = "payment-service")
    public void onCart(CartGetResponse response) {
        requestReplyClient.complete(response.correlationId(), response);
    }

    @KafkaListener(topics = Topics.SHIP_RATE_RESP, groupId = "payment-service")
    public void onShip(ShippingRateResponse resp) {
        requestReplyClient.complete(resp.correlationId(), resp);
    }

    @KafkaListener(topics = Topics.CATALOG_PROD_SNAPSHOT_RESP, groupId = "payment-service")
    public void onProd(ProductSnapshotResponse resp) {
        requestReplyClient.complete(resp.correlationId(), resp);
    }

    @KafkaListener(topics = Topics.CUST_ADDR_RESP, groupId = "payment-service")
    public void onAddr(AddressQueryResponse resp) {
        requestReplyClient.complete(resp.correlationId(), resp);
    }

}
