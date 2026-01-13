# EShop Microservices - Production-Grade Implementation Guide

## 📋 Mục lục

1. [Tổng quan kiến trúc](#1-tổng-quan-kiến-trúc)
2. [Kafka Configuration](#2-kafka-configuration)
3. [Redis Cache](#3-redis-cache)
4. [Outbox Pattern](#4-outbox-pattern)
5. [Saga Pattern](#5-saga-pattern)
6. [Thread Pool](#6-thread-pool)
7. [Docker Deployment](#7-docker-deployment)
8. [Hướng dẫn tích hợp](#8-hướng-dẫn-tích-hợp)

---

## 1. Tổng quan kiến trúc

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              API Gateway                                 │
│                            (Spring Cloud)                                │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼                           ▼                           ▼
┌───────────────┐           ┌───────────────┐           ┌───────────────┐
│ Catalog Svc   │           │ Checkout Svc  │           │  Order Svc    │
│               │◄─────────►│ (Saga Orch)   │◄─────────►│               │
└───────┬───────┘   Kafka   └───────┬───────┘   Kafka   └───────┬───────┘
        │                           │                           │
        │ Redis                     │ Redis                     │
        ▼                           ▼                           ▼
┌───────────────┐           ┌───────────────┐           ┌───────────────┐
│   MySQL DB    │           │   MySQL DB    │           │   MySQL DB    │
│ + Outbox Tbl  │           │ + Saga State  │           │ + Outbox Tbl  │
└───────────────┘           └───────────────┘           └───────────────┘
        │                           │                           │
        └───────────────────────────┴───────────────────────────┘
                                    │
                            ┌───────┴───────┐
                            │     Kafka     │
                            │   (Events)    │
                            └───────────────┘
```

---

## 2. Kafka Configuration

### 2.1 Producer Configuration

**File:** `common-service/src/main/java/com/eshop/common/kafka/config/KafkaProducerConfig.java`

**Key Features:**
- ✅ Idempotent producer (`enable.idempotence=true`)
- ✅ Strong durability (`acks=all`)
- ✅ Compression (`snappy`)
- ✅ Batching cho throughput cao

```java
// Cách sử dụng
@Autowired
private KafkaTemplate<String, Object> kafkaTemplate;

// Send với callback
kafkaTemplate.send(topic, key, event)
    .whenComplete((result, ex) -> {
        if (ex != null) {
            log.error("Send failed", ex);
        } else {
            log.info("Sent to partition={}, offset={}",
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
        }
    });
```

### 2.2 Consumer Configuration

**File:** `common-service/src/main/java/com/eshop/common/kafka/config/KafkaConsumerConfig.java`

**Key Features:**
- ✅ Dead Letter Queue (DLQ) cho failed messages
- ✅ Exponential backoff retry
- ✅ Manual offset commit
- ✅ Error handling deserializer

```java
@KafkaListener(
    topics = "order.created",
    groupId = "order-service-group",
    containerFactory = "kafkaListenerContainerFactory"
)
public void handle(ConsumerRecord<String, OrderCreatedEvent> record, 
                   Acknowledgment ack) {
    try {
        // Process message
        processOrder(record.value());
        
        // Acknowledge only after successful processing
        ack.acknowledge();
    } catch (Exception e) {
        // Don't ack - will retry with exponential backoff
        // After max retries -> DLQ
        throw e;
    }
}
```

### 2.3 Request-Reply Pattern với Circuit Breaker

**File:** `common-service/src/main/java/com/eshop/common/kafka/requestreply/KafkaRequestReplyClient.java`

```java
@Autowired
private KafkaRequestReplyClient requestReplyClient;

// Request với timeout và circuit breaker
ProductSnapshotResponse response = requestReplyClient.request(
    "catalog.product.snapshot.request",  // request topic
    "catalog.product.snapshot.response", // response topic
    ProductSnapshotResponse.class,
    correlationId -> new ProductSnapshotRequest(correlationId, productIds),
    Duration.ofSeconds(10)
);

// Request với fallback
ProductSnapshotResponse response = requestReplyClient.requestWithFallback(
    requestTopic,
    responseTopic,
    ProductSnapshotResponse.class,
    requestFactory,
    Duration.ofSeconds(10),
    defaultResponse  // Fallback nếu fail
);
```

---

## 3. Redis Cache

### 3.1 Configuration

**File:** `common-service/src/main/java/com/eshop/common/cache/config/RedisConfig.java`

**Key Features:**
- ✅ Connection pooling (Lettuce)
- ✅ Cluster support với topology refresh
- ✅ Proper JSON serialization với type info
- ✅ Configurable timeouts

### 3.2 Cache Service

**File:** `common-service/src/main/java/com/eshop/common/cache/RedisCacheService.java`

**Key Features:**
- ✅ NULL marker để prevent cache penetration
- ✅ Distributed lock để prevent cache stampede
- ✅ TTL jitter để prevent synchronized expiration
- ✅ Batch operations

```java
@Autowired
private RedisCacheService cacheService;

// Cache-aside với loader và distributed lock
Product product = cacheService.getOrLoad(
    CacheKeys.productById(productId),
    Product.class,
    Duration.ofMinutes(15),
    () -> productRepository.findById(productId).orElse(null)
);

// Distributed lock
cacheService.executeWithLock(
    "inventory:" + productId,
    Duration.ofSeconds(10),
    () -> {
        // Critical section - only one instance executes this
        return updateInventory(productId, quantity);
    }
);

// Batch operations
Map<String, Product> products = cacheService.multiGet(productKeys, Product.class);
```

### 3.3 Cache Key Convention

**File:** `common-service/src/main/java/com/eshop/common/cache/CacheKeys.java`

```
Format: {namespace}:{version}:{domain}:{entity}:{identifier}
Example: eshop:v1:catalog:product:123

// Usage
String key = CacheKeys.productById(123);       // eshop:v1:catalog:product:id:123
String key = CacheKeys.categoryTree();          // eshop:v1:catalog:category:tree
String key = CacheKeys.cartItems(customerId);   // eshop:v1:cart:items:456
```

---

## 4. Outbox Pattern

### 4.1 Concept

```
┌─────────────────────────────────────────────────────────────────┐
│                     Business Service                              │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              Single Database Transaction                  │    │
│  │  1. Save business data (Order)                           │    │
│  │  2. Save event to outbox_messages table                  │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Outbox Publisher                              │
│  - Polls outbox_messages table (every 500ms)                    │
│  - Publishes to Kafka                                           │
│  - Marks as PUBLISHED on success                                │
│  - Exponential backoff retry on failure                         │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 Usage

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OutboxService outboxService;
    
    @Transactional  // QUAN TRỌNG: Cùng transaction
    public Order createOrder(CreateOrderRequest request) {
        // 1. Save business data
        Order order = Order.builder()
            .orderNumber(generateOrderNumber())
            .customerId(request.getCustomerId())
            .status(OrderStatus.NEW)
            .build();
        
        order = orderRepository.save(order);
        
        // 2. Enqueue event to outbox (same transaction)
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .orderNumber(order.getOrderNumber())
            .customerId(order.getCustomerId())
            .total(order.getTotal())
            .createdAt(Instant.now())
            .build();
        
        outboxService.enqueue(
            KafkaTopicsConfig.ORDER_CREATED,  // topic
            "Order",                           // aggregate type
            order.getOrderNumber(),            // aggregate ID (Kafka key)
            event                              // event payload
        );
        
        return order;
    }
}
```

### 4.3 Database Schema

```sql
CREATE TABLE outbox_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL UNIQUE,
    topic VARCHAR(200) NOT NULL,
    message_key VARCHAR(200),
    event_type VARCHAR(500) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status ENUM('PENDING', 'IN_PROGRESS', 'PUBLISHED', 'FAILED', 'DEAD'),
    retry_count INT DEFAULT 0,
    next_retry_at TIMESTAMP(3),
    last_error VARCHAR(2000),
    created_at TIMESTAMP(3) NOT NULL,
    published_at TIMESTAMP(3),
    
    INDEX idx_status_retry (status, next_retry_at)
);
```

---

## 5. Saga Pattern

### 5.1 Concept

```
┌─────────────────────────────────────────────────────────────────┐
│                    Checkout Saga Orchestrator                     │
│                                                                   │
│  Step 1: VALIDATE_CART ────────────────────────────────────────► │
│  Step 2: GET_PRODUCT_SNAPSHOT ─────────────────────────────────► │
│  Step 3: VALIDATE_ADDRESS ─────────────────────────────────────► │
│  Step 4: CALCULATE_SHIPPING ───────────────────────────────────► │
│  Step 5: CREATE_ORDER ─────────────────────────────────────────► │
│  Step 6: PROCESS_PAYMENT ──────────────────────────────────────► │
│  Step 7: CONFIRM_ORDER ────────────────────────────────────────► │
│                                                                   │
│  On Failure at Step N:                                           │
│  ◄──────────────────── Compensate Step N-1                       │
│  ◄──────────────────── Compensate Step N-2                       │
│  ◄──────────────────── ... until Step 1                          │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 Define Saga

```java
@Component
public class CheckoutSagaDefinition {

    @PostConstruct
    public void register() {
        SagaDefinition<CheckoutSagaContext> definition = SagaDefinition.<CheckoutSagaContext>builder()
            .sagaType("CHECKOUT")
            .timeout(Duration.ofMinutes(15))
            .steps(List.of(
                SagaStepDefinition.<CheckoutSagaContext>builder()
                    .name("CREATE_ORDER")
                    .action(createOrderStep::execute)
                    .compensation(createOrderStep::compensate)  // Rollback logic
                    .timeout(Duration.ofMinutes(1))
                    .nextStep("PROCESS_PAYMENT")
                    .build(),
                // ... more steps
            ))
            .build();

        orchestrator.registerSaga(definition);
    }
}
```

### 5.3 Execute Saga

```java
@RestController
public class CheckoutController {

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@RequestBody CheckoutRequest request) {
        
        // Build context
        CheckoutSagaContext context = CheckoutSagaContext.builder()
            .customerId(request.getCustomerId())
            .addressId(request.getAddressId())
            .paymentMethod(request.getPaymentMethod())
            .build();
        
        // Start saga
        SagaState saga = orchestrator.startSaga(
            "CHECKOUT",
            "checkout-" + customerId + "-" + System.currentTimeMillis(),
            context,
            customerId
        );
        
        // Execute all steps
        saga = orchestrator.executeAll(saga.getSagaId(), CheckoutSagaContext.class);
        
        // Return result
        return ResponseEntity.ok(buildResponse(saga));
    }
}
```

---

## 6. Thread Pool

### 6.1 Configuration

**File:** `common-service/src/main/java/com/eshop/common/async/AsyncConfig.java`

```java
// IO-bound tasks (database, external APIs, file I/O)
// Sizing: cores * 2 to cores * 4
@Bean("ioTaskExecutor")
public ThreadPoolTaskExecutor ioTaskExecutor() {
    executor.setCorePoolSize(cores * 2);
    executor.setMaxPoolSize(cores * 4);
    executor.setQueueCapacity(500);
    executor.setRejectedExecutionHandler(new CallerRunsPolicy());
}

// CPU-bound tasks (computation, transformation)
// Sizing: cores to cores + 1
@Bean("cpuTaskExecutor")
public ThreadPoolTaskExecutor cpuTaskExecutor() {
    executor.setCorePoolSize(cores);
    executor.setMaxPoolSize(cores + 1);
    executor.setQueueCapacity(100);
    executor.setRejectedExecutionHandler(new AbortPolicy());
}
```

### 6.2 Usage

```java
@Service
public class ProductService {

    @Async("ioTaskExecutor")  // For database/API calls
    public CompletableFuture<Product> fetchProductAsync(Integer id) {
        return CompletableFuture.completedFuture(
            productRepository.findById(id).orElse(null)
        );
    }

    @Async("cpuTaskExecutor")  // For computation
    public CompletableFuture<Report> generateReportAsync(ReportRequest request) {
        // Heavy computation
        return CompletableFuture.completedFuture(report);
    }
}
```

---

## 7. Docker Deployment

### 7.1 Start Infrastructure

```bash
# Start MySQL, Redis, Kafka, Zookeeper
docker-compose up -d mysql redis zookeeper kafka

# Wait for healthy status
docker-compose ps

# Start monitoring (optional)
docker-compose --profile monitoring up -d
```

### 7.2 Start Services

```bash
# Start Spring Cloud infrastructure
docker-compose up -d config-server eureka-server gateway

# Start business services
docker-compose up -d catalog-service cart-service customer-service \
    order-service checkout-service payment-service shipping-service
```

### 7.3 Environment Variables

```bash
# .env file
MYSQL_ROOT_PASSWORD=root123
MYSQL_PASSWORD=eshop123
JWT_SECRET=your-super-secret-jwt-key-minimum-32-characters
PAYPAL_CLIENT_ID=your-paypal-client-id
PAYPAL_CLIENT_SECRET=your-paypal-secret
GRAFANA_PASSWORD=admin
```

---

## 8. Hướng dẫn tích hợp

### 8.1 Thêm dependency vào pom.xml

```xml
<dependency>
    <groupId>com.eshop</groupId>
    <artifactId>common-service</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Required dependencies -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
<dependency>
    <groupId>net.javacrumbs.shedlock</groupId>
    <artifactId>shedlock-spring</artifactId>
</dependency>
<dependency>
    <groupId>net.javacrumbs.shedlock</groupId>
    <artifactId>shedlock-provider-jdbc-template</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 8.2 Import Configurations

```java
@SpringBootApplication
@Import({
    KafkaProducerConfig.class,
    KafkaConsumerConfig.class,
    KafkaTopicsConfig.class,
    RedisConfig.class,
    AsyncConfig.class,
    ShedLockConfig.class
})
@EnableJpaRepositories(basePackages = {
    "com.eshop.common.outbox",
    "com.eshop.common.saga",
    "com.eshop.yourservice.repository"
})
public class YourServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourServiceApplication.class, args);
    }
}
```

### 8.3 Run Database Migrations

```bash
# Apply common tables (outbox, saga, shedlock)
mysql -u root -p < init-scripts/mysql/01-common-tables.sql
```

### 8.4 Checklist tích hợp

- [ ] Thêm dependencies vào pom.xml
- [ ] Import configurations trong Application class
- [ ] Chạy database migrations
- [ ] Cấu hình application.yml từ template
- [ ] Implement Kafka consumers với idempotency
- [ ] Sử dụng OutboxService cho event publishing
- [ ] Sử dụng RedisCacheService cho caching
- [ ] Define saga nếu có distributed transaction

---

## 📚 Best Practices

### Kafka
1. **Luôn dùng idempotent consumer** - Check `eventId` trước khi process
2. **Manual acknowledge** - Chỉ ack sau khi process thành công
3. **DLQ monitoring** - Setup alerts cho DLQ messages

### Redis
1. **NULL marker** - Prevent cache penetration attacks
2. **TTL jitter** - Avoid thundering herd
3. **Distributed lock** - Cho critical sections

### Outbox
1. **Same transaction** - Business data + outbox message
2. **Exponential backoff** - Cho retry
3. **Cleanup job** - Xóa old published messages

### Saga
1. **Idempotent steps** - Mỗi step phải idempotent
2. **Compensation** - Mỗi step cần compensation logic
3. **Timeout** - Set timeout cho saga và từng step

---

## 🔧 Troubleshooting

### Kafka không connect được
```bash
# Check Kafka is running
docker-compose logs kafka

# Test connectivity
docker exec -it eshop-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Redis connection timeout
```bash
# Check Redis
docker exec -it eshop-redis redis-cli ping

# Check pool config
spring.redis.lettuce.pool.max-active=16
```

### Outbox messages stuck
```sql
-- Check stuck messages
SELECT * FROM outbox_messages WHERE status = 'IN_PROGRESS' AND locked_at < NOW() - INTERVAL 5 MINUTE;

-- Reset stuck messages
UPDATE outbox_messages SET status = 'PENDING', locked_by = NULL, locked_at = NULL
WHERE status = 'IN_PROGRESS' AND locked_at < NOW() - INTERVAL 5 MINUTE;
```

---

## 📞 Support

Nếu có câu hỏi, vui lòng tạo issue hoặc liên hệ team.
