package com.eshop.common.async;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Production-grade Thread Pool Configuration
 * 
 * Features:
 * 1. Separate pools for different workload types (IO-bound vs CPU-bound)
 * 2. Context propagation (MDC, Request attributes)
 * 3. Metrics integration (Micrometer)
 * 4. Graceful shutdown
 * 5. Uncaught exception handling
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    private final MeterRegistry meterRegistry;

    @Value("${async.io.core-pool-size:#{T(java.lang.Runtime).getRuntime().availableProcessors() * 2}}")
    private int ioCorePoolSize;

    @Value("${async.io.max-pool-size:#{T(java.lang.Runtime).getRuntime().availableProcessors() * 4}}")
    private int ioMaxPoolSize;

    @Value("${async.io.queue-capacity:500}")
    private int ioQueueCapacity;

    @Value("${async.cpu.core-pool-size:#{T(java.lang.Runtime).getRuntime().availableProcessors()}}")
    private int cpuCorePoolSize;

    @Value("${async.cpu.max-pool-size:#{T(java.lang.Runtime).getRuntime().availableProcessors() + 1}}")
    private int cpuMaxPoolSize;

    @Value("${async.cpu.queue-capacity:100}")
    private int cpuQueueCapacity;

    @Value("${async.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Value("${async.await-termination-seconds:30}")
    private int awaitTerminationSeconds;

    public AsyncConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Default async executor (IO-bound tasks)
     * Used for: database calls, external API calls, file I/O
     */
    @Override
    @Bean("asyncExecutor")
    public Executor getAsyncExecutor() {
        return ioTaskExecutor();
    }

    /**
     * IO-bound task executor
     * 
     * Sizing strategy:
     * - Core pool: 2 * CPU cores (threads mostly waiting on I/O)
     * - Max pool: 4 * CPU cores (burst capacity)
     * - Queue: large queue to buffer requests during spikes
     * - Rejection: CallerRuns to provide backpressure
     */
    @Bean("ioTaskExecutor")
    public ThreadPoolTaskExecutor ioTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(ioCorePoolSize);
        executor.setMaxPoolSize(ioMaxPoolSize);
        executor.setQueueCapacity(ioQueueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("io-async-");
        
        // CallerRuns policy: when queue is full, caller thread executes the task
        // This provides natural backpressure
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // Context propagation decorator
        executor.setTaskDecorator(new ContextPropagatingTaskDecorator());
        
        // Graceful shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        
        executor.initialize();
        
        // Register metrics
        ExecutorServiceMetrics.monitor(
            meterRegistry,
            executor.getThreadPoolExecutor(),
            "io-async",
            Tags.of("type", "io-bound")
        );
        
        log.info("Configured IO task executor: core={}, max={}, queue={}", 
            ioCorePoolSize, ioMaxPoolSize, ioQueueCapacity);
        
        return executor;
    }

    /**
     * CPU-bound task executor
     * 
     * Sizing strategy:
     * - Core pool: CPU cores (one thread per core)
     * - Max pool: CPU cores + 1 (minimal overhead for context switching)
     * - Queue: small queue (work should be processed immediately)
     * - Rejection: Abort policy (fail fast when overloaded)
     */
    @Bean("cpuTaskExecutor")
    public ThreadPoolTaskExecutor cpuTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(cpuCorePoolSize);
        executor.setMaxPoolSize(cpuMaxPoolSize);
        executor.setQueueCapacity(cpuQueueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("cpu-async-");
        
        // Abort policy: reject task when overloaded (CPU tasks should fail fast)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        
        executor.setTaskDecorator(new ContextPropagatingTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        
        executor.initialize();
        
        ExecutorServiceMetrics.monitor(
            meterRegistry,
            executor.getThreadPoolExecutor(),
            "cpu-async",
            Tags.of("type", "cpu-bound")
        );
        
        log.info("Configured CPU task executor: core={}, max={}, queue={}", 
            cpuCorePoolSize, cpuMaxPoolSize, cpuQueueCapacity);
        
        return executor;
    }

    /**
     * Scheduler executor for @Scheduled tasks
     */
    @Bean("schedulerExecutor")
    public ThreadPoolTaskExecutor schedulerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("scheduler-");
        
        // Discard oldest policy for schedulers (skip old tasks)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        
        executor.initialize();
        
        ExecutorServiceMetrics.monitor(
            meterRegistry,
            executor.getThreadPoolExecutor(),
            "scheduler",
            Tags.of("type", "scheduler")
        );
        
        return executor;
    }

    /**
     * Kafka listener executor
     */
    @Bean("kafkaListenerExecutor")
    public ThreadPoolTaskExecutor kafkaListenerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("kafka-listener-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        
        ExecutorServiceMetrics.monitor(
            meterRegistry,
            executor.getThreadPoolExecutor(),
            "kafka-listener",
            Tags.of("type", "messaging")
        );
        
        return executor;
    }

    /**
     * Uncaught exception handler for async tasks
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("Uncaught async exception in method {}: {}",
                method.getName(),
                throwable.getMessage(),
                throwable);
            
            // Could publish to dead letter queue or alert system here
        };
    }

    /**
     * Task decorator that propagates context to async threads
     */
    public static class ContextPropagatingTaskDecorator implements TaskDecorator {
        
        @Override
        public Runnable decorate(Runnable runnable) {
            // Capture context from calling thread
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            Map<String, String> mdcContext = org.slf4j.MDC.getCopyOfContextMap();
            
            return () -> {
                try {
                    // Set context in async thread
                    if (requestAttributes != null) {
                        RequestContextHolder.setRequestAttributes(requestAttributes);
                    }
                    if (mdcContext != null) {
                        org.slf4j.MDC.setContextMap(mdcContext);
                    }
                    
                    runnable.run();
                    
                } finally {
                    // Clean up
                    RequestContextHolder.resetRequestAttributes();
                    org.slf4j.MDC.clear();
                }
            };
        }
    }
}
