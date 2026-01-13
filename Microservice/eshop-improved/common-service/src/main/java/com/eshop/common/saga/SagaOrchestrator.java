package com.eshop.common.saga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Saga Orchestrator - Executes saga steps in sequence
 * 
 * Features:
 * 1. Step-by-step execution with state persistence
 * 2. Automatic compensation on failure
 * 3. Timeout handling
 * 4. Idempotent execution
 * 5. Metrics and logging
 */
@Component
@Slf4j
public class SagaOrchestrator {

    private final SagaRepository sagaRepository;
    private final ObjectMapper objectMapper;
    
    // Metrics
    private final Counter sagaStartedCounter;
    private final Counter sagaCompletedCounter;
    private final Counter sagaFailedCounter;
    private final Counter sagaCompensatedCounter;
    private final Timer sagaExecutionTimer;
    private final Timer stepExecutionTimer;

    // Registered saga definitions
    private final Map<String, SagaDefinition<?>> sagaDefinitions = new HashMap<>();

    public SagaOrchestrator(
            SagaRepository sagaRepository,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.sagaRepository = sagaRepository;
        this.objectMapper = objectMapper;
        
        // Initialize metrics
        this.sagaStartedCounter = Counter.builder("saga.started")
            .description("Total sagas started")
            .register(meterRegistry);
        
        this.sagaCompletedCounter = Counter.builder("saga.completed")
            .description("Successfully completed sagas")
            .register(meterRegistry);
        
        this.sagaFailedCounter = Counter.builder("saga.failed")
            .description("Failed sagas")
            .register(meterRegistry);
        
        this.sagaCompensatedCounter = Counter.builder("saga.compensated")
            .description("Compensated sagas")
            .register(meterRegistry);
        
        this.sagaExecutionTimer = Timer.builder("saga.execution.time")
            .description("Total saga execution time")
            .register(meterRegistry);
        
        this.stepExecutionTimer = Timer.builder("saga.step.execution.time")
            .description("Step execution time")
            .register(meterRegistry);
    }

    /**
     * Register a saga definition
     */
    public <T> void registerSaga(SagaDefinition<T> definition) {
        sagaDefinitions.put(definition.getSagaType(), definition);
        log.info("Registered saga: {} with {} steps", 
            definition.getSagaType(), definition.getSteps().size());
    }

    /**
     * Start a new saga
     */
    @Transactional
    public <T> SagaState startSaga(
            String sagaType,
            String correlationId,
            T context,
            Integer userId) {
        
        @SuppressWarnings("unchecked")
        SagaDefinition<T> definition = (SagaDefinition<T>) sagaDefinitions.get(sagaType);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown saga type: " + sagaType);
        }

        // Check for existing saga with same correlation ID
        Optional<SagaState> existing = sagaRepository.findByCorrelationIdAndSagaType(
            correlationId, sagaType);
        if (existing.isPresent()) {
            SagaState existingSaga = existing.get();
            log.info("Found existing saga for correlationId={}, status={}", 
                correlationId, existingSaga.getStatus());
            return existingSaga;
        }

        try {
            String payload = objectMapper.writeValueAsString(context);
            String firstStep = definition.getSteps().get(0).getName();
            Duration timeout = definition.getTimeout();
            
            SagaState saga = SagaState.create(
                sagaType,
                correlationId,
                firstStep,
                payload,
                userId,
                Instant.now().plus(timeout)
            );
            
            saga = sagaRepository.save(saga);
            sagaStartedCounter.increment();
            
            log.info("Started saga: type={}, sagaId={}, correlationId={}, firstStep={}",
                sagaType, saga.getSagaId(), correlationId, firstStep);
            
            return saga;
            
        } catch (JsonProcessingException e) {
            throw new SagaException("Failed to serialize saga context", e);
        }
    }

    /**
     * Execute the current step of a saga
     */
    @Transactional
    public <T> SagaState executeStep(String sagaId, Class<T> contextType) {
        SagaState saga = sagaRepository.findBySagaId(sagaId)
            .orElseThrow(() -> new SagaException("Saga not found: " + sagaId));

        // Check status
        if (saga.getStatus() == SagaStatus.COMPLETED || 
            saga.getStatus() == SagaStatus.COMPENSATED) {
            log.debug("Saga {} already in terminal state: {}", sagaId, saga.getStatus());
            return saga;
        }

        // Check timeout
        if (saga.isTimedOut()) {
            log.warn("Saga {} timed out, initiating compensation", sagaId);
            return compensate(saga, "Saga timed out", contextType);
        }

        @SuppressWarnings("unchecked")
        SagaDefinition<T> definition = (SagaDefinition<T>) sagaDefinitions.get(saga.getSagaType());
        if (definition == null) {
            throw new SagaException("Unknown saga type: " + saga.getSagaType());
        }

        SagaStepDefinition<T> currentStep = definition.getStep(saga.getCurrentStep());
        if (currentStep == null) {
            throw new SagaException("Unknown step: " + saga.getCurrentStep());
        }

        Timer.Sample sample = Timer.start();
        
        try {
            // Deserialize context
            T context = objectMapper.readValue(saga.getPayload(), contextType);
            
            log.debug("Executing step {} for saga {}", currentStep.getName(), sagaId);
            
            // Execute step
            T updatedContext = currentStep.getAction().apply(saga, context);
            
            // Serialize updated context
            String updatedPayload = objectMapper.writeValueAsString(updatedContext);
            
            // Move to next step or complete
            String nextStep = currentStep.getNextStep();
            if (nextStep == null || definition.getStep(nextStep) == null) {
                // Final step - complete saga
                saga.moveToStep("COMPLETED", updatedPayload);
                saga.complete();
                sagaCompletedCounter.increment();
                log.info("Saga {} completed successfully", sagaId);
            } else {
                // Move to next step
                saga.moveToStep(nextStep, updatedPayload);
                log.debug("Saga {} moved to step {}", sagaId, nextStep);
            }
            
            return sagaRepository.save(saga);
            
        } catch (Exception e) {
            log.error("Step {} failed for saga {}: {}", currentStep.getName(), sagaId, e.getMessage());
            sagaFailedCounter.increment();
            return compensate(saga, e.getMessage(), contextType);
            
        } finally {
            sample.stop(stepExecutionTimer);
        }
    }

    /**
     * Execute all remaining steps of a saga
     */
    @Transactional
    public <T> SagaState executeAll(String sagaId, Class<T> contextType) {
        Timer.Sample sample = Timer.start();
        
        try {
            SagaState saga = sagaRepository.findBySagaId(sagaId)
                .orElseThrow(() -> new SagaException("Saga not found: " + sagaId));
            
            @SuppressWarnings("unchecked")
            SagaDefinition<T> definition = (SagaDefinition<T>) sagaDefinitions.get(saga.getSagaType());
            
            while (saga.getStatus() == SagaStatus.STARTED || 
                   saga.getStatus() == SagaStatus.IN_PROGRESS) {
                
                saga = executeStep(sagaId, contextType);
                
                // Check for terminal states
                if (saga.getStatus() == SagaStatus.COMPLETED ||
                    saga.getStatus() == SagaStatus.COMPENSATED ||
                    saga.getStatus() == SagaStatus.COMPENSATION_FAILED) {
                    break;
                }
            }
            
            return saga;
            
        } finally {
            sample.stop(sagaExecutionTimer);
        }
    }

    /**
     * Compensate (rollback) a failed saga
     */
    @Transactional
    public <T> SagaState compensate(SagaState saga, String reason, Class<T> contextType) {
        log.info("Starting compensation for saga {}: {}", saga.getSagaId(), reason);
        
        saga.fail(saga.getCurrentStep(), reason);
        sagaRepository.save(saga);

        @SuppressWarnings("unchecked")
        SagaDefinition<T> definition = (SagaDefinition<T>) sagaDefinitions.get(saga.getSagaType());
        
        try {
            T context = objectMapper.readValue(saga.getPayload(), contextType);
            
            // Get completed steps in reverse order
            List<String> completedSteps = parseCompletedSteps(saga.getCompletedSteps());
            Collections.reverse(completedSteps);
            
            for (String stepName : completedSteps) {
                SagaStepDefinition<T> step = definition.getStep(stepName);
                if (step != null && step.getCompensation() != null) {
                    try {
                        log.debug("Compensating step {} for saga {}", stepName, saga.getSagaId());
                        step.getCompensation().accept(saga, context);
                    } catch (Exception e) {
                        log.error("Compensation failed for step {}: {}", stepName, e.getMessage());
                        saga.compensationFailed(e.getMessage());
                        return sagaRepository.save(saga);
                    }
                }
            }
            
            saga.compensated();
            sagaCompensatedCounter.increment();
            log.info("Saga {} fully compensated", saga.getSagaId());
            
            return sagaRepository.save(saga);
            
        } catch (Exception e) {
            log.error("Failed to parse context during compensation: {}", e.getMessage());
            saga.compensationFailed(e.getMessage());
            return sagaRepository.save(saga);
        }
    }

    /**
     * Parse completed steps from JSON array string
     */
    private List<String> parseCompletedSteps(String completedStepsJson) {
        if (completedStepsJson == null || completedStepsJson.equals("[]")) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(completedStepsJson, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse completed steps: {}", completedStepsJson);
            return new ArrayList<>();
        }
    }

    /**
     * Get saga by ID
     */
    public Optional<SagaState> getSaga(String sagaId) {
        return sagaRepository.findBySagaId(sagaId);
    }

    /**
     * Get saga by correlation ID
     */
    public Optional<SagaState> getSagaByCorrelationId(String correlationId, String sagaType) {
        return sagaRepository.findByCorrelationIdAndSagaType(correlationId, sagaType);
    }

    /**
     * Custom exception for saga errors
     */
    public static class SagaException extends RuntimeException {
        public SagaException(String message) {
            super(message);
        }
        
        public SagaException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

/**
 * Saga Definition - defines steps for a saga type
 */
@lombok.Data
@lombok.Builder
class SagaDefinition<T> {
    private String sagaType;
    private List<SagaStepDefinition<T>> steps;
    
    @lombok.Builder.Default
    private Duration timeout = Duration.ofMinutes(30);
    
    public SagaStepDefinition<T> getStep(String name) {
        return steps.stream()
            .filter(s -> s.getName().equals(name))
            .findFirst()
            .orElse(null);
    }
}
