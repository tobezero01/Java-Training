package com.eshop.common.saga;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Definition of a saga step
 * 
 * @param <T> Saga context type
 */
@Data
@Builder
public class SagaStepDefinition<T> {
    
    /**
     * Step name (unique within saga)
     */
    private String name;
    
    /**
     * Step execution logic
     * Returns updated context or throws exception on failure
     */
    private BiFunction<SagaState, T, T> action;
    
    /**
     * Compensation logic (rollback)
     * Called when this step or later steps fail
     */
    private BiConsumer<SagaState, T> compensation;
    
    /**
     * Timeout for this step
     */
    @Builder.Default
    private Duration timeout = Duration.ofMinutes(5);
    
    /**
     * Maximum retries for this step
     */
    @Builder.Default
    private int maxRetries = 3;
    
    /**
     * Whether this step can be skipped on retry
     */
    @Builder.Default
    private boolean idempotent = true;
    
    /**
     * Next step name (null for final step)
     */
    private String nextStep;
    
    /**
     * Whether this step requires async execution
     */
    @Builder.Default
    private boolean async = false;
    
    /**
     * Create a simple step with action only (no compensation)
     */
    public static <T> SagaStepDefinition<T> of(
            String name, 
            BiFunction<SagaState, T, T> action) {
        return SagaStepDefinition.<T>builder()
            .name(name)
            .action(action)
            .build();
    }
    
    /**
     * Create a step with action and compensation
     */
    public static <T> SagaStepDefinition<T> of(
            String name, 
            BiFunction<SagaState, T, T> action,
            BiConsumer<SagaState, T> compensation) {
        return SagaStepDefinition.<T>builder()
            .name(name)
            .action(action)
            .compensation(compensation)
            .build();
    }
}
