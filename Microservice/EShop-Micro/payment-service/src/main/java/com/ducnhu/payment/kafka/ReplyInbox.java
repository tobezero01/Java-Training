package com.ducnhu.payment.kafka;

import com.ducnhu.common.events.settings.EmailSettingsResponse;
import com.ducnhu.common.events.settings.PaypalSettingsResponse;
import com.ducnhu.common.kafka.RequestReplyClient;
import com.ducnhu.common.kafka.Topics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReplyInbox {

    private final RequestReplyClient rr;

    // Nhận cấu hình PayPal cho PaypalService.settings()
    @KafkaListener(topics = Topics.SETTINGS_PAYPAL_RESP, groupId = "payment-service")
    public void onPaypal(PaypalSettingsResponse resp) {
        rr.complete(resp.correlationId(), resp);
    }

    @KafkaListener(topics = Topics.SETTINGS_EMAIL_RESP, groupId = "payment-service")
    public void onEmail(EmailSettingsResponse resp) {
        rr.complete(resp.correlationId(), resp);
    }
}
