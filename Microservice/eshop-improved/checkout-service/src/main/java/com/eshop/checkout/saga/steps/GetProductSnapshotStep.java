package com.eshop.checkout.saga.steps;

import com.eshop.checkout.saga.CheckoutSagaContext;
import com.eshop.checkout.saga.CheckoutSagaContext.ProductSnapshot;
import com.eshop.common.kafka.config.KafkaTopicsConfig;
import com.eshop.common.kafka.requestreply.KafkaRequestReplyClient;
import com.eshop.common.saga.SagaState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/**
 * Step 2: Get Product Snapshot
 * 
 * Fetches current product info/prices from Catalog Service.
 * This ensures we use the most up-to-date prices for the order.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetProductSnapshotStep {

    private final KafkaRequestReplyClient requestReplyClient;

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    public CheckoutSagaContext execute(SagaState saga, CheckoutSagaContext context) {
        log.info("Executing GET_PRODUCT_SNAPSHOT for saga={}, productCount={}", 
            saga.getSagaId(), context.getCartItems().size());

        context.validate("GET_PRODUCT_SNAPSHOT");

        // Get product IDs from cart
        List<Integer> productIds = context.getCartItems().stream()
            .map(CheckoutSagaContext.CartItemSnapshot::getProductId)
            .toList();

        // Request product snapshots from Catalog Service
        ProductSnapshotResponse response = requestReplyClient.request(
            KafkaTopicsConfig.PRODUCT_SNAPSHOT_REQUEST,
            KafkaTopicsConfig.PRODUCT_SNAPSHOT_RESPONSE,
            ProductSnapshotResponse.class,
            correlationId -> new ProductSnapshotRequest(correlationId, productIds),
            TIMEOUT
        );

        if (response.getError() != null) {
            throw new RuntimeException("Failed to get product snapshots: " + response.getError());
        }

        // Validate all products are available
        List<ProductSnapshot> snapshots = response.getProducts().stream()
            .map(p -> ProductSnapshot.builder()
                .productId(p.getProductId())
                .name(p.getName())
                .alias(p.getAlias())
                .price(p.getPrice())
                .discountPercent(p.getDiscountPercent())
                .finalPrice(calculateFinalPrice(p.getPrice(), p.getDiscountPercent()))
                .stockQuantity(p.getStockQuantity())
                .inStock(p.isInStock())
                .mainImage(p.getMainImage())
                .build())
            .toList();

        // Validate stock availability
        for (var cartItem : context.getCartItems()) {
            ProductSnapshot product = snapshots.stream()
                .filter(p -> p.getProductId().equals(cartItem.getProductId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                    "Product not found: " + cartItem.getProductId()));

            if (!product.isInStock()) {
                throw new RuntimeException("Product out of stock: " + product.getName());
            }

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException(String.format(
                    "Insufficient stock for %s: requested %d, available %d",
                    product.getName(), cartItem.getQuantity(), product.getStockQuantity()));
            }

            // Update cart item with current price
            cartItem.setCurrentPrice(product.getFinalPrice());
        }

        context.setProductSnapshots(snapshots);
        context.setSubtotal(context.calculateSubtotal());

        log.info("GET_PRODUCT_SNAPSHOT completed: {} products, subtotal={}", 
            snapshots.size(), context.getSubtotal());

        return context;
    }

    private BigDecimal calculateFinalPrice(BigDecimal price, BigDecimal discountPercent) {
        if (discountPercent == null || discountPercent.compareTo(BigDecimal.ZERO) <= 0) {
            return price;
        }
        BigDecimal discount = price.multiply(discountPercent).divide(BigDecimal.valueOf(100));
        return price.subtract(discount);
    }

    // Request/Response DTOs
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ProductSnapshotRequest {
        private String correlationId;
        private List<Integer> productIds;
    }

    @lombok.Data
    public static class ProductSnapshotResponse {
        private String correlationId;
        private List<ProductData> products;
        private String error;
    }

    @lombok.Data
    public static class ProductData {
        private Integer productId;
        private String name;
        private String alias;
        private BigDecimal price;
        private BigDecimal discountPercent;
        private Integer stockQuantity;
        private boolean inStock;
        private String mainImage;
    }
}
