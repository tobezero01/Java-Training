package com.ducnhu.shipping.kafka;

import com.ducnhu.common.events.shipping.ShippingRateRequest;
import com.ducnhu.common.events.shipping.ShippingRateResponse;
import com.ducnhu.common.kafka.Topics;
import com.ducnhu.shipping.entity.ShippingRate;
import com.ducnhu.shipping.service.ShippingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShippingMessageHandler {
    private final ShippingQueryService shippingQueryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = Topics.SHIP_RATE_REQ, groupId = "shipping-service")
    public void onReq(ShippingRateRequest request) {
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
