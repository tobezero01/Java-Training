package com.eshop.checkout.saga.steps;

import com.eshop.checkout.saga.CheckoutSagaContext;
import com.eshop.checkout.saga.CheckoutSagaContext.CartItemSnapshot;
import com.eshop.common.kafka.config.KafkaTopicsConfig;
import com.eshop.common.kafka.requestreply.KafkaRequestReplyClient;
import com.eshop.common.saga.SagaState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Step 1: Validate Cart
 * 
 * Fetches cart items from Cart Service via Kafka Request-Reply
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ValidateCartStep {

    private final KafkaRequestReplyClient requestReplyClient;

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    public CheckoutSagaContext execute(SagaState saga, CheckoutSagaContext context) {
        log.info("Executing VALIDATE_CART for saga={}, customerId={}", 
            saga.getSagaId(), context.getCustomerId());

        context.validate("VALIDATE_CART");

        // Request cart items from Cart Service
        CartItemsResponse response = requestReplyClient.request(
            KafkaTopicsConfig.CART_GET_REQUEST,
            KafkaTopicsConfig.CART_GET_RESPONSE,
            CartItemsResponse.class,
            correlationId -> new CartItemsRequest(correlationId, context.getCustomerId()),
            TIMEOUT
        );

        if (response.getItems() == null || response.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        // Map to cart item snapshots
        List<CartItemSnapshot> cartItems = response.getItems().stream()
            .map(item -> CartItemSnapshot.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .build())
            .toList();

        context.setCartItems(cartItems);
        context.setTotalQuantity(cartItems.stream().mapToInt(CartItemSnapshot::getQuantity).sum());

        log.info("VALIDATE_CART completed: {} items, total quantity={}", 
            cartItems.size(), context.getTotalQuantity());

        return context;
    }

    // Request/Response DTOs
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class CartItemsRequest {
        private String correlationId;
        private Integer customerId;
    }

    @lombok.Data
    public static class CartItemsResponse {
        private String correlationId;
        private List<CartItem> items;
        private String error;
    }

    @lombok.Data
    public static class CartItem {
        private Integer productId;
        private String productName;
        private Integer quantity;
        private java.math.BigDecimal unitPrice;
    }
}
