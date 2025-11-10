package com.ducnhu.settings.kafka;


import com.ducnhu.common.events.settings.EmailSettingsRequest;
import com.ducnhu.common.events.settings.EmailSettingsResponse;
import com.ducnhu.common.events.settings.PaypalSettingsRequest;
import com.ducnhu.common.events.settings.PaypalSettingsResponse;
import com.ducnhu.common.kafka.Topics;
import com.ducnhu.settings.entity.Setting;
import com.ducnhu.settings.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettingsHandlers {
    private final SettingRepository settingRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = Topics.SETTINGS_EMAIL_REQ, groupId = "settings-service")
    public void onEmail(EmailSettingsRequest emailSettingsRequest) {
        EmailSettingsResponse response = new EmailSettingsResponse(
                emailSettingsRequest.correlationId(),
                getOrNull("MAIL_HOST"), getInt("MAIL_PORT"), getOrNull("MAIL_USERNAME"), getOrNull("MAIL_PASSWORD"),
                getOrNull("SMTP_AUTH"), getOrNull("SMTP_SECURED"),
                getOrNull("MAIL_FROM"), getOrNull("MAIL_SENDER_NAME"),
                getOrNull("ORDER_CONFIRMATION_SUBJECT"), getOrNull("ORDER_CONFIRMATION_CONTENT"),
                getOrNull("CUSTOMER_VERIFY_SUBJECT"), getOrNull("CUSTOMER_VERIFY_CONTENT")
        );
        kafkaTemplate.send(Topics.SETTINGS_EMAIL_RESP, response);
    }

    @KafkaListener(topics = Topics.SETTINGS_PAYPAL_REQ, groupId = "settings-service")
    public void onPaypal(PaypalSettingsRequest request) {
        PaypalSettingsResponse response = new PaypalSettingsResponse(
                request.correlationId(),
                getOrNull("PAYPAL_API_BASE_URL"),
                getOrNull("PAYPAL_API_CLIENT_ID"),
                getOrNull("PAYPAL_API_CLIENT_SECRET")
        );
        kafkaTemplate.send(Topics.SETTINGS_PAYPAL_RESP, response);
    }

    private String getOrNull(String key) {
        Setting setting = settingRepository.findByKey(key);
        return setting == null ? null : setting.getValue();
    }

    private Integer getInt(String key) {
        String str = getOrNull(key);
        return str == null ? null : Integer.valueOf(str);
    }

}
