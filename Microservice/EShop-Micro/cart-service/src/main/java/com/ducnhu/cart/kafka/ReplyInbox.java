package com.ducnhu.cart.kafka;

import com.ducnhu.common.events.catalog.ProductSnapshotResponse;
import com.ducnhu.common.kafka.RequestReplyClient;
import com.ducnhu.common.kafka.Topics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReplyInbox {
    private final RequestReplyClient requestReplyClient;

    @KafkaListener(topics = Topics.CATALOG_PROD_SNAPSHOT_RESP, groupId = "cart-service")
    public void onSnapshot(ProductSnapshotResponse resp) {
        requestReplyClient.complete(resp.correlationId(), resp);
    }
}
