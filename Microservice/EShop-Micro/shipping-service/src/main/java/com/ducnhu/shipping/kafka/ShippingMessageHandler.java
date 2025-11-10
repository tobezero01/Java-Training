package com.ducnhu.shipping.kafka;

import com.ducnhu.common.events.shipping.ShippingRateRequest;
import com.ducnhu.common.events.shipping.ShippingRateResponse;
import com.ducnhu.common.kafka.Topics;
import com.ducnhu.shipping.entity.ShippingRate;
import com.ducnhu.shipping.service.ShippingQueryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShippingMessageHandler {
    private final ShippingQueryService shippingQueryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    @KafkaListener(
            topics = Topics.SHIP_RATE_REQ,
            containerFactory = "kafkaListenerContainerFactory"
            // groupId có thể để trống để lấy từ spring.kafka.consumer.group-id trong YAML
            // groupId = "shipping-service"
    )
    public void onReq(ShippingRateRequest request) throws JsonProcessingException {
        ShippingRate rate = shippingQueryService.get(request.countryId(), request.stateOrCity());
        ShippingRateResponse response = new ShippingRateResponse(
                request.correlationId(), request.countryId(),
                request.stateOrCity(),
                rate == null ? 0f : rate.getRate(),
                rate == null ? 0 : rate.getDays(),
                rate != null && Boolean.TRUE.equals(rate.getCodSupported())
        );
        kafkaTemplate.send(Topics.SHIP_RATE_RESP, response);
    }

}
