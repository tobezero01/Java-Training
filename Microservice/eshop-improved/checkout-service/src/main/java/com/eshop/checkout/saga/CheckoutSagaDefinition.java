package com.eshop.checkout.saga;

import com.eshop.common.saga.*;
import com.eshop.common.kafka.config.KafkaTopicsConfig;
import com.eshop.common.outbox.OutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Checkout Saga Definition
 * 
 * Orchestrates the checkout process across multiple services:
 * 1. VALIDATE_CART - Validate cart items exist and have stock
 * 2. GET_PRODUCT_SNAPSHOT - Get current product info/prices
 * 3. VALIDATE_ADDRESS - Validate shipping address
 * 4. CALCULATE_SHIPPING - Calculate shipping cost
 * 5. CREATE_ORDER - Create order record
 * 6. PROCESS_PAYMENT - Process payment (COD or PayPal)
 * 7. CONFIRM_ORDER - Confirm order and clear cart
 * 
 * Compensation actions (rollback):
 * - CONFIRM_ORDER fails → Cancel order, release inventory
 * - PROCESS_PAYMENT fails → Cancel order
 * - CREATE_ORDER fails → Nothing to compensate
 * - etc.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CheckoutSagaDefinition {

    private final SagaOrchestrator orchestrator;
    private final ObjectMapper objectMapper;
    
    // Step handlers
    private final ValidateCartStep validateCartStep;
    private final GetProductSnapshotStep getProductSnapshotStep;
    private final ValidateAddressStep validateAddressStep;
    private final CalculateShippingStep calculateShippingStep;
    private final CreateOrderStep createOrderStep;
    private final ProcessPaymentStep processPaymentStep;
    private final ConfirmOrderStep confirmOrderStep;

    public static final String SAGA_TYPE = "CHECKOUT";

    @PostConstruct
    public void register() {
        SagaDefinition<CheckoutSagaContext> definition = SagaDefinition.<CheckoutSagaContext>builder()
            .sagaType(SAGA_TYPE)
            .timeout(Duration.ofMinutes(15))
            .steps(List.of(
                // Step 1: Validate Cart
                SagaStepDefinition.<CheckoutSagaContext>builder()
                    .name("VALIDATE_CART")
                    .action(validateCartStep::execute)
                    .compensation(null) // No compensation needed
                    .timeout(Duration.ofSeconds(30))
                    .nextStep("GET_PRODUCT_SNAPSHOT")
                    .build(),
                
                // Step 2: Get Product Snapshot
                SagaStepDefinition.<CheckoutSagaContext>builder()
                    .name("GET_PRODUCT_SNAPSHOT")
                    .action(getProductSnapshotStep::execute)
                    .compensation(null)
                    .timeout(Duration.ofSeconds(30))
                    .nextStep("VALIDATE_ADDRESS")
                    .build(),
                
                // Step 3: Validate Address
                SagaStepDefinition.<CheckoutSagaContext>builder()
                    .name("VALIDATE_ADDRESS")
                    .action(validateAddressStep::execute)
                    .compensation(null)
                    .timeout(Duration.ofSeconds(30))
                    .nextStep("CALCULATE_SHIPPING")
                    .build(),
                
                // Step 4: Calculate Shipping
                SagaStepDefinition.<CheckoutSagaContext>builder()
                    .name("CALCULATE_SHIPPING")
                    .action(calculateShippingStep::execute)
                    .compensation(null)
                    .timeout(Duration.ofSeconds(30))
                    .nextStep("CREATE_ORDER")
                    .build(),
                
                // Step 5: Create Order
                SagaStepDefinition.<CheckoutSagaContext>builder()
                    .name("CREATE_ORDER")
                    .action(createOrderStep::execute)
                    .compensation(createOrderStep::compensate)
                    .timeout(Duration.ofMinutes(1))
                    .nextStep("PROCESS_PAYMENT")
                    .build(),
                
                // Step 6: Process Payment
                SagaStepDefinition.<CheckoutSagaContext>builder()
                    .name("PROCESS_PAYMENT")
                    .action(processPaymentStep::execute)
                    .compensation(processPaymentStep::compensate)
                    .timeout(Duration.ofMinutes(5))
                    .nextStep("CONFIRM_ORDER")
                    .build(),
                
                // Step 7: Confirm Order (final step)
                SagaStepDefinition.<CheckoutSagaContext>builder()
                    .name("CONFIRM_ORDER")
                    .action(confirmOrderStep::execute)
                    .compensation(confirmOrderStep::compensate)
                    .timeout(Duration.ofMinutes(1))
                    .nextStep(null) // Final step
                    .build()
            ))
            .build();

        orchestrator.registerSaga(definition);
        log.info("Registered Checkout Saga with {} steps", definition.getSteps().size());
    }
}
