package com.eshop.checkout.controller;

import com.eshop.checkout.saga.CheckoutSagaContext;
import com.eshop.checkout.saga.CheckoutSagaDefinition;
import com.eshop.checkout.service.CheckoutService;
import com.eshop.common.saga.SagaOrchestrator;
import com.eshop.common.saga.SagaState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Optional;

/**
 * Checkout API Controller
 * 
 * Handles checkout requests and saga status queries.
 */
@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Checkout", description = "Checkout operations")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final SagaOrchestrator sagaOrchestrator;

    /**
     * Start checkout process
     */
    @PostMapping
    @Operation(summary = "Start checkout", description = "Initiates the checkout saga")
    public ResponseEntity<CheckoutResponse> startCheckout(
            @Valid @RequestBody CheckoutRequest request,
            @RequestHeader("X-Customer-Id") Integer customerId) {
        
        log.info("Starting checkout for customerId={}, paymentMethod={}", 
            customerId, request.getPaymentMethod());

        // Build saga context
        CheckoutSagaContext context = CheckoutSagaContext.builder()
            .customerId(customerId)
            .addressId(request.getAddressId())
            .paymentMethod(request.getPaymentMethod())
            .paypalOrderId(request.getPaypalOrderId())
            .couponCode(request.getCouponCode())
            .notes(request.getNotes())
            .build();

        // Start saga
        SagaState saga = sagaOrchestrator.startSaga(
            CheckoutSagaDefinition.SAGA_TYPE,
            "checkout-" + customerId + "-" + System.currentTimeMillis(),
            context,
            customerId
        );

        // Execute saga synchronously (for simple cases)
        // For async execution, use messaging
        saga = sagaOrchestrator.executeAll(saga.getSagaId(), CheckoutSagaContext.class);

        // Build response
        CheckoutResponse response = buildResponse(saga);

        if ("COMPLETED".equals(saga.getStatus().name())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get checkout status
     */
    @GetMapping("/{sagaId}")
    @Operation(summary = "Get checkout status", description = "Returns the current status of a checkout saga")
    public ResponseEntity<CheckoutResponse> getCheckoutStatus(@PathVariable String sagaId) {
        Optional<SagaState> saga = sagaOrchestrator.getSaga(sagaId);
        
        if (saga.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(buildResponse(saga.get()));
    }

    /**
     * Get checkout preview (without creating order)
     */
    @PostMapping("/preview")
    @Operation(summary = "Preview checkout", description = "Returns checkout summary without creating order")
    public ResponseEntity<CheckoutPreviewResponse> previewCheckout(
            @Valid @RequestBody CheckoutPreviewRequest request,
            @RequestHeader("X-Customer-Id") Integer customerId) {
        
        log.info("Previewing checkout for customerId={}", customerId);

        CheckoutPreviewResponse preview = checkoutService.preview(customerId, request);
        
        return ResponseEntity.ok(preview);
    }

    /**
     * Retry failed checkout
     */
    @PostMapping("/{sagaId}/retry")
    @Operation(summary = "Retry checkout", description = "Retries a failed checkout saga")
    public ResponseEntity<CheckoutResponse> retryCheckout(@PathVariable String sagaId) {
        Optional<SagaState> saga = sagaOrchestrator.getSaga(sagaId);
        
        if (saga.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SagaState currentSaga = saga.get();
        
        // Only allow retry for failed sagas
        if (!"FAILED".equals(currentSaga.getStatus().name()) && 
            !"COMPENSATION_FAILED".equals(currentSaga.getStatus().name())) {
            return ResponseEntity.badRequest()
                .body(CheckoutResponse.builder()
                    .sagaId(sagaId)
                    .status(currentSaga.getStatus().name())
                    .error("Cannot retry saga in status: " + currentSaga.getStatus())
                    .build());
        }

        // Re-execute from current step
        currentSaga = sagaOrchestrator.executeStep(sagaId, CheckoutSagaContext.class);
        
        return ResponseEntity.ok(buildResponse(currentSaga));
    }

    private CheckoutResponse buildResponse(SagaState saga) {
        CheckoutResponse.CheckoutResponseBuilder builder = CheckoutResponse.builder()
            .sagaId(saga.getSagaId())
            .status(saga.getStatus().name())
            .currentStep(saga.getCurrentStep());

        if (saga.getFailedStep() != null) {
            builder.failedStep(saga.getFailedStep())
                   .error(saga.getFailureReason());
        }

        // Extract order info from payload if completed
        if ("COMPLETED".equals(saga.getStatus().name()) && saga.getPayload() != null) {
            try {
                CheckoutSagaContext context = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(saga.getPayload(), CheckoutSagaContext.class);
                
                builder.orderNumber(context.getOrderNumber())
                       .total(context.getTotal())
                       .paymentStatus(context.getPaymentStatus());
            } catch (Exception e) {
                log.warn("Failed to parse saga payload", e);
            }
        }

        return builder.build();
    }

    // Request/Response DTOs
    @lombok.Data
    public static class CheckoutRequest {
        @jakarta.validation.constraints.NotNull
        private Integer addressId;
        
        @jakarta.validation.constraints.NotBlank
        private String paymentMethod; // "COD" or "PAYPAL"
        
        private String paypalOrderId; // Required if paymentMethod is PAYPAL
        private String couponCode;
        private String notes;
    }

    @lombok.Data
    public static class CheckoutPreviewRequest {
        @jakarta.validation.constraints.NotNull
        private Integer addressId;
        private String couponCode;
    }

    @lombok.Data
    @lombok.Builder
    public static class CheckoutResponse {
        private String sagaId;
        private String status;
        private String currentStep;
        private String failedStep;
        private String error;
        private String orderNumber;
        private java.math.BigDecimal total;
        private String paymentStatus;
    }

    @lombok.Data
    @lombok.Builder
    public static class CheckoutPreviewResponse {
        private java.util.List<CheckoutSagaContext.CartItemSnapshot> items;
        private CheckoutSagaContext.AddressSnapshot shippingAddress;
        private java.math.BigDecimal subtotal;
        private java.math.BigDecimal shippingCost;
        private java.math.BigDecimal discount;
        private java.math.BigDecimal tax;
        private java.math.BigDecimal total;
        private Integer estimatedDeliveryDays;
    }
}
