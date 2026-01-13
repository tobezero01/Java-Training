package com.eshop.checkout.saga;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Checkout Saga Context
 * 
 * Holds all data needed throughout the checkout saga.
 * This object is serialized and stored in saga state.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSagaContext {
    
    // ===== Input data (from checkout request) =====
    private Integer customerId;
    private Integer addressId;
    private String paymentMethod; // "COD" or "PAYPAL"
    private String paypalOrderId; // For PayPal payment
    private String couponCode;
    private String notes;
    
    // ===== Cart data =====
    private List<CartItemSnapshot> cartItems;
    private int totalQuantity;
    
    // ===== Product snapshot (from Catalog service) =====
    private List<ProductSnapshot> productSnapshots;
    
    // ===== Address data (from Customer service) =====
    private AddressSnapshot shippingAddress;
    
    // ===== Shipping data (from Shipping service) =====
    private BigDecimal shippingCost;
    private Integer shippingDays;
    private String shippingMethod;
    
    // ===== Order data =====
    private String orderNumber;
    private Long orderId;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal total;
    
    // ===== Payment data =====
    private String paymentId;
    private String paymentStatus;
    private Instant paymentTime;
    
    // ===== Error tracking =====
    private String errorMessage;
    private String failedStep;

    /**
     * Cart item snapshot
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemSnapshot {
        private Integer productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice; // Price at time of add to cart
        private BigDecimal currentPrice; // Current price from catalog
    }

    /**
     * Product snapshot from Catalog service
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSnapshot {
        private Integer productId;
        private String name;
        private String alias;
        private BigDecimal price;
        private BigDecimal discountPercent;
        private BigDecimal finalPrice;
        private Integer stockQuantity;
        private boolean inStock;
        private String mainImage;
    }

    /**
     * Address snapshot from Customer service
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressSnapshot {
        private Integer addressId;
        private String fullName;
        private String phoneNumber;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String postalCode;
        private Integer countryId;
        private String countryName;
    }

    /**
     * Calculate subtotal from product snapshots
     */
    public BigDecimal calculateSubtotal() {
        if (productSnapshots == null || productSnapshots.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return cartItems.stream()
            .map(item -> {
                ProductSnapshot product = productSnapshots.stream()
                    .filter(p -> p.getProductId().equals(item.getProductId()))
                    .findFirst()
                    .orElse(null);
                
                if (product != null) {
                    return product.getFinalPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                }
                return BigDecimal.ZERO;
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate total including shipping and discount
     */
    public BigDecimal calculateTotal() {
        BigDecimal sub = subtotal != null ? subtotal : calculateSubtotal();
        BigDecimal ship = shippingCost != null ? shippingCost : BigDecimal.ZERO;
        BigDecimal disc = discount != null ? discount : BigDecimal.ZERO;
        BigDecimal t = tax != null ? tax : BigDecimal.ZERO;
        
        return sub.add(ship).subtract(disc).add(t);
    }

    /**
     * Validate context has required data for next step
     */
    public void validate(String step) {
        switch (step) {
            case "VALIDATE_CART":
                if (customerId == null) {
                    throw new IllegalStateException("Customer ID is required");
                }
                break;
            case "GET_PRODUCT_SNAPSHOT":
                if (cartItems == null || cartItems.isEmpty()) {
                    throw new IllegalStateException("Cart items are required");
                }
                break;
            case "VALIDATE_ADDRESS":
                if (addressId == null) {
                    throw new IllegalStateException("Address ID is required");
                }
                break;
            case "CREATE_ORDER":
                if (productSnapshots == null || shippingAddress == null) {
                    throw new IllegalStateException("Product snapshots and address are required");
                }
                break;
            case "PROCESS_PAYMENT":
                if (orderNumber == null) {
                    throw new IllegalStateException("Order number is required");
                }
                if ("PAYPAL".equals(paymentMethod) && paypalOrderId == null) {
                    throw new IllegalStateException("PayPal order ID is required");
                }
                break;
        }
    }
}
