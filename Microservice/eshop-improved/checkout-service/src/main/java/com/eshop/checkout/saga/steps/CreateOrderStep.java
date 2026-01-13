package com.eshop.checkout.saga.steps;

import com.eshop.checkout.saga.CheckoutSagaContext;
import com.eshop.common.kafka.config.KafkaTopicsConfig;
import com.eshop.common.outbox.OutboxService;
import com.eshop.common.saga.SagaState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * Step 5: Create Order
 * 
 * Creates the order record in Order Service.
 * Has compensation to cancel the order if later steps fail.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreateOrderStep {

    private final OutboxService outboxService;
    private final OrderRepository orderRepository; // Giả định có repository

    public CheckoutSagaContext execute(SagaState saga, CheckoutSagaContext context) {
        log.info("Executing CREATE_ORDER for saga={}, customerId={}", 
            saga.getSagaId(), context.getCustomerId());

        context.validate("CREATE_ORDER");

        // Generate order number
        String orderNumber = generateOrderNumber();
        
        // Calculate totals
        BigDecimal subtotal = context.getSubtotal();
        BigDecimal shipping = context.getShippingCost();
        BigDecimal discount = context.getDiscount() != null ? context.getDiscount() : BigDecimal.ZERO;
        BigDecimal tax = calculateTax(subtotal); // 10% VAT
        BigDecimal total = subtotal.add(shipping).subtract(discount).add(tax);

        // Create order entity
        Order order = Order.builder()
            .orderNumber(orderNumber)
            .customerId(context.getCustomerId())
            .status(OrderStatus.NEW)
            .paymentMethod(context.getPaymentMethod())
            .subtotal(subtotal)
            .shippingCost(shipping)
            .discount(discount)
            .tax(tax)
            .total(total)
            // Shipping address
            .shippingName(context.getShippingAddress().getFullName())
            .shippingPhone(context.getShippingAddress().getPhoneNumber())
            .shippingAddress(formatAddress(context.getShippingAddress()))
            .shippingCity(context.getShippingAddress().getCity())
            .shippingState(context.getShippingAddress().getState())
            .shippingPostalCode(context.getShippingAddress().getPostalCode())
            .shippingCountryId(context.getShippingAddress().getCountryId())
            // Timestamps
            .createdAt(Instant.now())
            .expectedDeliveryDays(context.getShippingDays())
            .notes(context.getNotes())
            .build();

        // Create order items
        List<OrderItem> orderItems = context.getCartItems().stream()
            .map(cartItem -> {
                var product = context.getProductSnapshots().stream()
                    .filter(p -> p.getProductId().equals(cartItem.getProductId()))
                    .findFirst()
                    .orElseThrow();
                
                return OrderItem.builder()
                    .productId(cartItem.getProductId())
                    .productName(product.getName())
                    .productImage(product.getMainImage())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(product.getFinalPrice())
                    .subtotal(product.getFinalPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                    .build();
            })
            .toList();

        order.setItems(orderItems);

        // Save order (in real implementation)
        // order = orderRepository.save(order);

        // Publish OrderCreatedEvent via Outbox
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .orderNumber(orderNumber)
            .customerId(context.getCustomerId())
            .total(total)
            .status(OrderStatus.NEW.name())
            .createdAt(Instant.now())
            .build();

        outboxService.enqueue(
            KafkaTopicsConfig.ORDER_CREATED,
            "Order",
            orderNumber,
            event
        );

        // Update context
        context.setOrderNumber(orderNumber);
        context.setOrderId(order.getId());
        context.setSubtotal(subtotal);
        context.setShippingCost(shipping);
        context.setDiscount(discount);
        context.setTax(tax);
        context.setTotal(total);

        log.info("CREATE_ORDER completed: orderNumber={}, total={}", orderNumber, total);

        return context;
    }

    /**
     * Compensation: Cancel the order
     */
    @Transactional
    public void compensate(SagaState saga, CheckoutSagaContext context) {
        log.info("Compensating CREATE_ORDER for saga={}, orderNumber={}", 
            saga.getSagaId(), context.getOrderNumber());

        if (context.getOrderNumber() == null) {
            log.warn("No order number to compensate");
            return;
        }

        // Update order status to CANCELLED
        // orderRepository.updateStatus(context.getOrderNumber(), OrderStatus.CANCELLED);

        // Publish OrderCancelledEvent via Outbox
        OrderCancelledEvent event = OrderCancelledEvent.builder()
            .orderNumber(context.getOrderNumber())
            .customerId(context.getCustomerId())
            .reason("Checkout saga compensation: " + context.getFailedStep())
            .cancelledAt(Instant.now())
            .build();

        outboxService.enqueue(
            KafkaTopicsConfig.ORDER_CANCELLED,
            "Order",
            context.getOrderNumber(),
            event
        );

        log.info("CREATE_ORDER compensated: orderNumber={}", context.getOrderNumber());
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "ORD-" + timestamp + "-" + random;
    }

    private BigDecimal calculateTax(BigDecimal subtotal) {
        return subtotal.multiply(BigDecimal.valueOf(0.10)); // 10% VAT
    }

    private String formatAddress(CheckoutSagaContext.AddressSnapshot addr) {
        StringBuilder sb = new StringBuilder();
        sb.append(addr.getAddressLine1());
        if (addr.getAddressLine2() != null && !addr.getAddressLine2().isEmpty()) {
            sb.append(", ").append(addr.getAddressLine2());
        }
        return sb.toString();
    }

    // Entity and DTO classes
    @lombok.Data
    @lombok.Builder
    public static class Order {
        private Long id;
        private String orderNumber;
        private Integer customerId;
        private OrderStatus status;
        private String paymentMethod;
        private BigDecimal subtotal;
        private BigDecimal shippingCost;
        private BigDecimal discount;
        private BigDecimal tax;
        private BigDecimal total;
        private String shippingName;
        private String shippingPhone;
        private String shippingAddress;
        private String shippingCity;
        private String shippingState;
        private String shippingPostalCode;
        private Integer shippingCountryId;
        private Instant createdAt;
        private Integer expectedDeliveryDays;
        private String notes;
        private List<OrderItem> items;
    }

    @lombok.Data
    @lombok.Builder
    public static class OrderItem {
        private Integer productId;
        private String productName;
        private String productImage;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }

    public enum OrderStatus {
        NEW, PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED, REFUNDED
    }

    @lombok.Data
    @lombok.Builder
    public static class OrderCreatedEvent {
        private String orderNumber;
        private Integer customerId;
        private BigDecimal total;
        private String status;
        private Instant createdAt;
    }

    @lombok.Data
    @lombok.Builder
    public static class OrderCancelledEvent {
        private String orderNumber;
        private Integer customerId;
        private String reason;
        private Instant cancelledAt;
    }

    // Placeholder interface
    public interface OrderRepository {
        Order save(Order order);
        void updateStatus(String orderNumber, OrderStatus status);
    }
}
