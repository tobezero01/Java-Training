package com.ducnhu.cart.kafka;

import com.ducnhu.cart.entity.CartItem;
import com.ducnhu.cart.service.ShoppingCartService;
import com.ducnhu.common.events.carts.CartClearCommand;
import com.ducnhu.common.events.carts.CartGetRequest;
import com.ducnhu.common.events.carts.CartGetResponse;
import com.ducnhu.common.events.carts.CartLine;
import com.ducnhu.common.kafka.RequestReplyClient;
import com.ducnhu.common.kafka.Topics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CartMessageHandler {
    private final ShoppingCartService shoppingCartService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RequestReplyClient requestReplyClient;

    @KafkaListener(topics = Topics.CART_GET_REQ, groupId = "cart-service")
    public void onGetCart(CartGetRequest request) {
        List<CartLine> items = shoppingCartService.list(request.customerId()).stream().map(this::toLine).collect(Collectors.toList());
        CartGetResponse resp = new CartGetResponse(request.correlationId(), request.customerId(), items);
        kafkaTemplate.send(Topics.CART_GET_RESP, resp);
    }

    @KafkaListener(topics = Topics.CART_CLEAR_CMD, groupId = "cart-service")
    public void onClear(CartClearCommand cmd) {
        shoppingCartService.clear(cmd.customerId());
    }

    private CartLine toLine(CartItem i) {
        return new CartLine(
                i.getProductId(), i.getName(), i.getAlias(), i.getImage(),
                i.getPrice(), i.getDiscountPrice(), i.getCost(),
                i.getLength(), i.getWidth(), i.getHeight(), i.getWeight(),
                i.getQuantity()
        );
    }
}
